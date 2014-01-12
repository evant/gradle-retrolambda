package me.tatarka

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.util.CollectionUtils

/**
 * Created with IntelliJ IDEA.
 * User: evan
 * Date: 8/4/13
 * Time: 1:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class RetrolambdaPluginAndroid implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.afterEvaluate {
            def sdkDir = project.android.plugin.sdkDirectory
            def androidJar = "$sdkDir/platforms/${project.android.compileSdkVersion}/android.jar"

            def buildPath = "${project.buildDir}/retrolambda"
            def jarPath = "$buildPath/${project.android.compileSdkVersion}"

            project.android.applicationVariants.each { var ->
                if (project.retrolambda.isIncluded(var.name)) {
                    def name = var.name.capitalize()

                    def inputDir = "$project.buildDir/retrolambda/$var.name"
                    def outputDir = var.javaCompile.destinationDir

                    def retroClasspath = CollectionUtils.join(File.pathSeparator,
                            (var.javaCompile.classpath + project.files(inputDir) + project.files(androidJar)).files)

                    var.javaCompile.destinationDir = project.file(inputDir)

                    var.javaCompile.sourceCompatibility = "1.8"
                    var.javaCompile.targetCompatibility = "1.8"
                    var.javaCompile.options.compilerArgs += ["-bootclasspath", "$jarPath/android.jar"]

                    project.task("compileRetrolambda${name}", dependsOn: ["compile${name}Java", "patchAndroidJar"], type: JavaExec) {
                        inputs.dir inputDir
                        outputs.dir outputDir
                        classpath = project.files(project.configurations.retrolambdaConfig)
                        main = 'net.orfjackal.retrolambda.Main'
                        jvmArgs = [
                                "-Dretrolambda.inputDir=$inputDir",
                                "-Dretrolambda.outputDir=$outputDir",
                                "-Dretrolambda.classpath=$retroClasspath",
                                "-Dretrolambda.bytecodeVersion=${project.retrolambda.bytecodeVersion}",
                                "-javaagent:${classpath.asPath}"
                        ]
                    }

                    // Set the output dir back so subsequent tasks use it
                    project.tasks.getByName("compile${name}Java").doLast {
                        var.javaCompile.destinationDir = outputDir
                    }

                    project.tasks.getByName("dex${name}").dependsOn("compileRetrolambda${name}")
                }
            }

            project.task('patchAndroidJar') {
                def rt = "${project.retrolambda.jdk}/jre/lib/rt.jar"

                def jdkPathError = " does not exist, make sure that JAVE_HOME or retrolambda.jdk points to a valid version of java8\n You can download java8 from https://jdk8.java.net/download.html"

                project.copy {
                    from project.file(androidJar)
                    into project.file(jarPath)
                }

                if (!project.file(rt).exists()) {
                    throw new RuntimeException(rt + jdkPathError)
                }

                project.copy {
                    from(project.zipTree(project.file(rt))) {
                        include("java/lang/invoke/**/*.class")
                    }

                    into project.file("$buildPath/classes")
                }

                if (!project.file("$buildPath/classes").isDirectory()) {
                    throw new RuntimeException("$buildPath/classes" + jdkPathError)
                }

                project.ant.jar(update: true, destFile: "$jarPath/android.jar") {
                    fileset(dir: "$buildPath/classes")
                }
            }
        }
    }
}
