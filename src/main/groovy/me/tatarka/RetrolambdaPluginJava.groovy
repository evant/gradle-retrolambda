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
import org.gradle.api.tasks.SourceSet

import static me.tatarka.RetrolambdaPlugin.checkIfExecutableExists
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
            project.sourceSets.all { SourceSet set ->
                if (project.retrolambda.isIncluded(set.name)) {
                    def name = set.name.capitalize()
                    def taskName = "compileRetrolambda$name"
                    def oldOutputDir = set.output.classesDir

                    set.output.classesDir = "$project.buildDir/retrolambda/$set.name"

                    project.task(taskName, dependsOn: set.classesTaskName, type: RetrolambdaTask) {
                        inputDir = set.output.classesDir
                        outputDir = oldOutputDir
                        classpath = set.compileClasspath + project.files(set.output.classesDir)
                        javaVersion = project.retrolambda.javaVersion
                    }

                    // Set the output dir back so subsequent tasks use it
                    project.tasks.getByName(set.classesTaskName).doLast {
                        set.output.classesDir = oldOutputDir
                    }

                    if (!project.retrolambda.onJava8) {
                        // Set JDK 8 for compiler task
                        project.tasks.getByName(set.compileJavaTaskName).doFirst {
                            it.options.fork = true
                            it.options.forkOptions.executable = "${project.retrolambda.tryGetJdk()}/bin/javac"
                        }
                    }
                }
            }

            project.tasks.getByName("jar").dependsOn("compileRetrolambda")
            project.tasks.getByName("javadoc").dependsOn("compileRetrolambda")
            project.tasks.getByName("test").dependsOn("compileRetrolambda").doFirst {
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
