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
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

import static me.tatarka.RetrolambdaPlugin.javaVersionToBytecode
/**
 * A task that runs retrolambda
 */
class RetrolambdaTask extends DefaultTask {
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

        logging.captureStandardOutput(LogLevel.INFO)

        if (!inputs.incremental || !changes.isEmpty()) {
            RetrolambdaExec retrolambdaExec = new RetrolambdaExec(project)
            retrolambdaExec.with {
                inputDir = this.inputDir
                outputDir = this.outputDir
                bytecodeVersion = javaVersionToBytecode(javaVersion)
                classpath = this.classpath
                if (inputs.incremental && retrolambda.incremental) {
                    includedFiles = project.files(changes*.file)
                }
                defaultMethods = retrolambda.defaultMethods
                jvmArgs = this.jvmArgs
            }
            retrolambdaExec.exec()
        }

        inputs.removed { change ->
            File outFile = toOutput(change.file)
            outFile.delete()
            project.logger.debug("Deleted " + outFile)
            deleteRelated(outFile)
        }
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
}
