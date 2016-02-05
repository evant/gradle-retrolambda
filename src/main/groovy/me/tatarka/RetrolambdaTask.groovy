/**
 Copyright 2014 Evan Tatarka

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package me.tatarka
import org.gradle.api.DefaultTask
import org.gradle.api.JavaVersion
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.api.tasks.incremental.InputFileDetails
import org.gradle.util.VersionNumber

import static me.tatarka.RetrolambdaPlugin.checkIfExecutableExists
import static me.tatarka.RetrolambdaPlugin.javaVersionToBytecode

/**
 * Created by evan on 3/4/14.
 */
class RetrolambdaTask extends DefaultTask {

    private static final int COMMANDLINE_LENGTH_LIMIT = 3496;

    @InputDirectory
    File inputDir

    @OutputDirectory
    File outputDir

    @Input
    FileCollection classpath

    @Input
    JavaVersion javaVersion = JavaVersion.VERSION_1_6

    @Input
    List<String> jvmArgs = []
    
    @TaskAction
    def execute(IncrementalTaskInputs inputs) {
        RetrolambdaExtension retrolambda = project.retrolambda
        
        def changes = []
        inputs.outOfDate { changes += it }
        
        // Ensure output is cleared if build is not incremental.
        if (inputs.incremental && !changes.isEmpty() && !retrolambda.incremental) {
            outputDir.eachFile { it.delete() }
        } else {
            changes.each { change ->
                if (change.modified) deleteRelated(toOutput(change.file))
            }
        }

        if (!inputs.incremental || !changes.isEmpty()) {
            project.javaexec {
                
                // Ensure retrolambda runs on java8
                if (!project.retrolambda.onJava8) {
                    def java = "${retrolambda.tryGetJdk()}/bin/java"
                    if (!checkIfExecutableExists(java)) {
                        throw new ProjectConfigurationException("Cannot find executable: $java", null)
                    }
                    executable java
                }

                def bytecodeVersion = javaVersionToBytecode(javaVersion)

                classpath = project.files(project.configurations.retrolambdaConfig)
                def path = this.classpath.asPath
                main = 'net.orfjackal.retrolambda.Main'
                jvmArgs = [
                        "-Dretrolambda.inputDir=$inputDir",
                        "-Dretrolambda.outputDir=$outputDir",
                        "-Dretrolambda.bytecodeVersion=$bytecodeVersion",
                ]

                def requiresJavaAgent = !requireVersion('1.6.0')
                if (requiresJavaAgent) {
                    jvmArgs += "-javaagent:$path"
                }

                def supportIncludeFiles = requireVersion('2.1.0')

                if (supportIncludeFiles && classpathLengthGreaterThanLimit(path)) {
                    def classpathFile = File.createTempFile("inc-", ".path")
                    classpathFile.withWriter('UTF-8') { writer ->
                        for (String item : this.classpath) {
                            writer.write(item + "\n")
                        }
                    }
                    classpathFile.deleteOnExit();
                    jvmArgs += "-Dretrolambda.classpathFile=${classpathFile.absolutePath}"
                } else {
                    jvmArgs += "-Dretrolambda.classpath=${path}"
                }

                if (inputs.incremental && retrolambda.incremental) {
                    if (supportIncludeFiles && changeFileLengthGreaterThanLimit(changes)) {
                        def includedFile = File.createTempFile("inc-", ".list")
                        includedFile.withWriter('UTF-8') { writer ->
                            for (InputFileDetails change : changes) {
                                writer.write(change.file.toString() + "\n")
                            }
                        }
                        includedFile.deleteOnExit();
                        jvmArgs += "-Dretrolambda.includedFilesFile=${includedFile.absolutePath}"
                    } else {
                        jvmArgs += "-Dretrolambda.includedFiles=${changes*.file.join(File.pathSeparator)}"
                    }
                }

                if (retrolambda.defaultMethods) {
                    jvmArgs += "-Dretrolambda.defaultMethods=true"
                }

                this.jvmArgs.each { arg -> jvmArgs += arg }

                logging.captureStandardOutput(LogLevel.INFO)
            }
        }

        inputs.removed { change ->
            File outFile = toOutput(change.file)
            outFile.delete()
            project.logger.debug("Deleted " + outFile)
            deleteRelated(outFile)
        }
    }

    def requireVersion(String version, boolean fallback = false) {
        def retrolambdaConfig = project.configurations.retrolambdaConfig
        def retrolambdaDep = retrolambdaConfig.dependencies.iterator().next()
        if (!retrolambdaDep.version) {
            // Don't know version, assume fallback.
            return fallback
        }
        def versionNumber = VersionNumber.parse(retrolambdaDep.version)
        def targetVersionNumber = VersionNumber.parse(version)
        versionNumber >= targetVersionNumber
    }

    def File toOutput(File file) {
        return outputDir.toPath().resolve(inputDir.toPath().relativize(file.toPath())).toFile()
    }

    def deleteRelated(File file) {
        def className = file.name.replaceFirst(/\.class$/, '')
        // Delete any generated Lambda classes
        project.logger.debug("Deleting related for " + className + " in " + file.parentFile)
        file.parentFile.eachFile {
            if (it.name.matches(/$className\$\$/ + /Lambda.*\.class$/)) {
                project.logger.debug("Deleted " + it)
                it.delete()
            }
        }
    }

    def static boolean classpathLengthGreaterThanLimit(String path) {
        return path.length() > COMMANDLINE_LENGTH_LIMIT
    }

    def static boolean changeFileLengthGreaterThanLimit(List<InputFileDetails> changes) {
        int total = 0;
        for (InputFileDetails change : changes) {
            total += change.file.toString().length();
            if (total > COMMANDLINE_LENGTH_LIMIT) {
                return true
            }
        }
        return false
    }
}
