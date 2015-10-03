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
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.Task
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.JavaCompile

import static me.tatarka.RetrolambdaPlugin.checkIfExecutableExists
/**
 * Created with IntelliJ IDEA.
 * User: evan
 * Date: 8/4/13
 * Time: 1:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class RetrolambdaPluginGroovy implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.afterEvaluate {
            project.sourceSets.all { SourceSet set ->
                if (project.retrolambda.isIncluded(set.name)) {
                    def name = set.name.capitalize()
                    def taskName = "compileRetrolambdaGroovy$name"
                    def oldOutputDir = set.output.classesDir
                    def newOutputDir = project.file("$project.buildDir/retrolambda/$set.name")

                    /* No compileJavaTaskName present, so re-use any modifications applied to compileJava and remap to Groovy */
                    def compileGroovyTaskName = set.compileJavaTaskName.replace(/Java/,/Groovy/)
                    def compileGroovyTask = project.tasks.getByName(compileGroovyTaskName)

                    compileGroovyTask.destinationDir = newOutputDir
                    def retrolambdaTask = project.task(taskName, dependsOn: compileGroovyTask, type: RetrolambdaTask) {
                        inputDir = newOutputDir
                        outputDir = oldOutputDir
                        classpath = set.compileClasspath + project.files(newOutputDir)
                        javaVersion = project.retrolambda.javaVersion
                        jvmArgs = project.retrolambda.jvmArgs
                    }

                    project.gradle.taskGraph.beforeTask { Task task
                        if (task == retrolambdaTask) {
                            retrolambdaTask.setEnabled(!set.allJava.isEmpty())
                        }
                    }

                    project.tasks.findByName(set.classesTaskName).dependsOn(retrolambdaTask)

                    if (!project.retrolambda.onJava8) {
                        // Set JDK 8 for compiler task
                        compileGroovyTask.doFirst {
                            it.options.fork = true
                            it.options.forkOptions.executable = "${project.retrolambda.tryGetJdk()}/bin/javac"
                        }
                    }
                }
            }

            project.tasks.getByName("test").doFirst {
                if (project.retrolambda.onJava8) {
                    //Ensure the tests run on java6/7
                    def oldJava = "${project.retrolambda.tryGetOldJdk()}/bin/java"
                    if (!checkIfExecutableExists(oldJava)) throw new ProjectConfigurationException("Cannot find executable: $oldJava", null)
                    executable oldJava
                }
            }
        }
    }
}
