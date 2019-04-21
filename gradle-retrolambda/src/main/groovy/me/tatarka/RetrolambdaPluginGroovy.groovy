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

import static me.tatarka.RetrolambdaPlugin.checkIfExecutableExists

public class RetrolambdaPluginGroovy implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.afterEvaluate {
            def retrolambda = project.extensions.getByType(RetrolambdaExtension)

            project.sourceSets.all { SourceSet set ->
                if (project.retrolambda.isIncluded(set.name)) {
                    def name = RetrolambdaUtil.capitalize(set.name)
                    def taskName = "compileRetrolambdaGroovy$name"
                    def oldOutputDir = RetrolambdaUtil.groovyOutputDir(set)
                    def newOutputDir = project.file("$project.buildDir/retrolambda/$set.name")

                    /* No compileJavaTaskName present, so re-use any modifications applied to compileJava and remap to Groovy */
                    def compileGroovyTaskName = set.compileJavaTaskName.replace(/Java/, /Groovy/)
                    def compileGroovyTask = project.tasks.getByName(compileGroovyTaskName)

                    compileGroovyTask.destinationDir = newOutputDir
                    def retrolambdaTask = project.task(taskName, dependsOn: compileGroovyTask, type: RetrolambdaTask) {
                        inputDir = newOutputDir
                        outputDir = oldOutputDir
                        classpath = set.compileClasspath + project.files(newOutputDir)
                        javaVersion = retrolambda.javaVersion
                        jvmArgs = retrolambda.jvmArgs
                    }

                    // enable retrolambdaTask dynamically, based on up-to-date source set before running 
                    project.gradle.taskGraph.beforeTask { Task task ->
                        if (task == retrolambdaTask) {
                            retrolambdaTask.setEnabled(!set.allJava.isEmpty())
                        }
                    }

                    project.tasks.findByName(set.classesTaskName).dependsOn(retrolambdaTask)

                    if (!project.retrolambda.onJava8) {
                        // Set JDK 8 for compiler task
                        compileGroovyTask.doFirst {
                            it.options.fork = true
                            it.options.forkOptions.executable = "${retrolambda.tryGetJdk()}/bin/javac"
                        }
                    }
                }
            }

            project.tasks.getByName("test").doFirst {
                if (retrolambda.onJava8) {
                    //Run tests on java6/7 if the property is defined.
                    String oldJdkPath = retrolambda.oldJdk
                    if (oldJdkPath != null) {
                        def oldJava = "$oldJdkPath/bin/java"
                        if (!checkIfExecutableExists(oldJava)) {
                            throw new ProjectConfigurationException("Cannot find executable: $oldJava", null)
                        }
                        task.executable oldJava
                    }
                }
            }
        }
    }
}
