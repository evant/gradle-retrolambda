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

import static me.tatarka.RetrolambdaPlugin.checkIfExecutableExists
import static me.tatarka.RetrolambdaPlugin.javaVersionToBytecode

/**
 * Created by evan on 3/4/14.
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

    @TaskAction
    def execute(IncrementalTaskInputs inputs) {
        def changes = []
        inputs.outOfDate { changes += it }

        changes.each { change ->
            if (change.modified) deleteRelated(toOutput(change.file))
        }

        if (!inputs.incremental || !changes.isEmpty()) {
            project.javaexec {
                // Ensure retrolambda runs on java8
                if (!project.retrolambda.onJava8) {
                    def java = "${project.retrolambda.tryGetJdk()}/bin/java"
                    if (!checkIfExecutableExists(java)) {
                        throw new ProjectConfigurationException("Cannot find executable: $java", null)
                    }
                    executable java
                }

                def bytecodeVersion = javaVersionToBytecode(javaVersion)

                classpath = project.files(project.configurations.retrolambdaConfig)
                main = 'net.orfjackal.retrolambda.Main'
                jvmArgs = [
                        "-Dretrolambda.inputDir=$inputDir",
                        "-Dretrolambda.outputDir=$outputDir",
                        "-Dretrolambda.classpath=${this.classpath.asPath}",
                        "-Dretrolambda.bytecodeVersion=$bytecodeVersion",
                        "-javaagent:$classpath.asPath"
                ]

                if (inputs.incremental) {
                    jvmArgs += "-Dretrolambda.changed=${changes*.file.join(File.pathSeparator)}"
                }

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

    def File toOutput(File file) {
        return outputDir.toPath().resolve(inputDir.toPath().relativize(file.toPath())).toFile()
    }

    def deleteRelated(File file) {
        def className = file.name.replaceFirst(/\.class$/, '')
        // Delete any generated Lambda classes
        project.logger.debug("Deleting related for " + className + " in " + file.parentFile)
        file.parentFile.eachFile {
            if (it.path.matches(/.*$className\$\$/ + /Lambda.*\.class$/)) {
                project.logger.debug("Deleted " + it)
                it.delete()
            }
        }
    }
}
