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
                    def inputDir = set.output.classesDir
                    def retroClasspath = set.runtimeClasspath.getAsPath()

                    project.task("compileRetrolambda${set.name}", dependsOn: 'classes', type: JavaExec) {
                        classpath = project.files(project.configurations.retrolambdaConfig)
                        main = 'net.orfjackal.retrolambda.Main'
                        jvmArgs = [
                                "-Dretrolambda.inputDir=$inputDir",
                                "-Dretrolambda.classpath=$retroClasspath",
                                "-Dretrolambda.bytecodeVersion=${project.retrolambda.bytecodeVersion}",
                                "-javaagent:${classpath.getAsPath()}"
                        ]
                    }
                }
            }

            project.tasks.getByName("jar").dependsOn("compileRetrolambda")
            project.tasks.getByName("javadoc").dependsOn("compileRetrolambda")
            project.tasks.getByName("test").dependsOn("compileRetrolambda")
        }
    }
}
