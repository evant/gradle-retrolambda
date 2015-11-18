package me.tatarka

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.process.JavaExecSpec
import org.gradle.util.VersionNumber

import static me.tatarka.RetrolambdaPlugin.checkIfExecutableExists

/**
 * Runs retrolambda with the given args, used by {@link RetrolambdaTask} and {@link RetrolambdaTransform}.
 */
@CompileStatic
class RetrolambdaExec {
    FileCollection classpath
    File inputDir;
    File outputDir;
    FileCollection includedFiles
    List<String> jvmArgs;
    int bytecodeVersion;
    boolean defaultMethods;

    private final Project project;

    RetrolambdaExec(Project project) {
        this.project = project;
    }

    public void exec() {
        project.javaexec { JavaExecSpec exec ->
            // Ensure retrolambda runs on java8
            def retrolambda = project.extensions.getByType(RetrolambdaExtension.class)
            def retrolambdaConfig = project.configurations.getByName("retrolambdaConfig")

            if (!retrolambda.onJava8) {
                def java = "${retrolambda.tryGetJdk()}/bin/java"
                if (!checkIfExecutableExists(java)) {
                    throw new ProjectConfigurationException("Cannot find executable: $java", null)
                }
                exec.executable java
            }

            exec.classpath = project.files(retrolambdaConfig)
            exec.main = 'net.orfjackal.retrolambda.Main'
            exec.jvmArgs = [
                    "-Dretrolambda.inputDir=$inputDir",
                    "-Dretrolambda.outputDir=$outputDir",
                    "-Dretrolambda.classpath=${classpath.asPath}",
                    "-Dretrolambda.bytecodeVersion=$bytecodeVersion",
            ]

            if (requiresJavaAgent(retrolambdaConfig)) {
                exec.jvmArgs "-javaagent:$classpath.asPath"
            }

            if (includedFiles != null) {
                def includedArg = "-Dretrolambda.includedFiles=${includedFiles.join(File.pathSeparator)}"
                exec.jvmArgs includedArg
                project.logger.quiet(includedArg)
            }

            if (defaultMethods) {
                exec.jvmArgs "-Dretrolambda.defaultMethods=true"
            }

            jvmArgs.each { arg -> exec.jvmArgs arg }
        }
    }

    private static boolean requiresJavaAgent(Configuration retrolambdaConfig) {
        retrolambdaConfig.resolve()
        def retrolambdaDep = retrolambdaConfig.dependencies.iterator().next()
        if (!retrolambdaDep.version) {
            // Don't know version, assume we need the javaagent.
            return true
        }
        def versionNumber = VersionNumber.parse(retrolambdaDep.version)
        def targetVersionNumber = VersionNumber.parse('1.6.0')
        versionNumber < targetVersionNumber
    }
}
