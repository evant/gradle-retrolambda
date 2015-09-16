package me.tatarka
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.file.FileCollection
import org.gradle.util.VersionNumber

import static me.tatarka.RetrolambdaPlugin.checkIfExecutableExists
/**
 * Runs retrolambda with the given args, used by {@link RetrolambdaTask} and {@link RetrolambdaTransform}.
 */
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
        project.javaexec {
            // Ensure retrolambda runs on java8
            if (!project.retrolambda.onJava8) {
                def java = "${project.retrolambda.tryGetJdk()}/bin/java"
                if (!checkIfExecutableExists(java)) {
                    throw new ProjectConfigurationException("Cannot find executable: $java", null)
                }
                executable java
            }

            classpath = project.files(project.configurations.retrolambdaConfig)
            main = 'net.orfjackal.retrolambda.Main'
            jvmArgs = [
                    "-Dretrolambda.inputDir=$inputDir",
                    "-Dretrolambda.outputDir=$outputDir",
                    "-Dretrolambda.classpath=${this.classpath.asPath}",
                    "-Dretrolambda.bytecodeVersion=$bytecodeVersion",
            ]

            if (requiresJavaAgent()) {
                jvmArgs += "-javaagent:$classpath.asPath"
            }

            if (includedFiles != null) {
                jvmArgs += "-Dretrolambda.includedFiles=${includedFiles.join(File.pathSeparator)}"
            }

            if (defaultMethods) {
                jvmArgs += "-Dretrolambda.defaultMethods=true"
            }

            this.jvmArgs.each { arg -> jvmArgs += arg }
        }
    }

    private boolean requiresJavaAgent() {
        def retrolambdaConfig = project.configurations.retrolambdaConfig
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
