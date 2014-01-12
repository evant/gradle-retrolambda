package me.tatarka

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec

/**
 * Created with IntelliJ IDEA.
 * User: evan
 * Date: 8/4/13
 * Time: 1:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class RetrolambdaPluginJava implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.afterEvaluate {
            project.sourceSets.all { set ->
                if (project.retrolambda.isIncluded(set.name)) {
                    def name = set.name.capitalize()
                    def inputDir = "$project.buildDir/retrolambda/$set.name"
                    def outputDir = set.output.classesDir
                    def retroClasspath = set.runtimeClasspath.getAsPath()
                    def taskName = "compileRetrolambda$name"

                    set.output.classesDir = inputDir

                    project.task(taskName, dependsOn: 'classes', type: JavaExec) {
                        inputs.dir inputDir
                        outputs.dir outputDir
                        classpath = project.files(project.configurations.retrolambdaConfig)
                        main = 'net.orfjackal.retrolambda.Main'
                        jvmArgs = [
                                "-Dretrolambda.inputDir=$inputDir",
                                "-Dretrolambda.outputDir=$outputDir",
                                "-Dretrolambda.classpath=$retroClasspath",
                                "-Dretrolambda.bytecodeVersion=${project.retrolambda.bytecodeVersion}",
                                "-javaagent:${classpath.getAsPath()}"
                        ]
                    }

                    // Set the output dir back so subsequent tasks use it
                    project.tasks.getByName("classes").doLast {
                        set.output.classesDir = outputDir
                    }
                }
            }

            project.tasks.getByName("jar").dependsOn("compileRetrolambda")
            project.tasks.getByName("javadoc").dependsOn("compileRetrolambda")
            project.tasks.getByName("test").dependsOn("compileRetrolambda")
        }
    }
}
