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
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.compile.JavaCompile

import static me.tatarka.RetrolambdaPlugin.checkIfExecutableExists

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
            def sdkDir

            Properties properties = new Properties()
            File localProps = project.rootProject.file('local.properties')
            if (localProps.exists()) {
                properties.load(localProps.newDataInputStream())
                sdkDir = properties.getProperty('sdk.dir')
            } else {
                sdkDir = System.getenv('ANDROID_HOME')
            }

            if (!sdkDir) {
                throw new ProjectConfigurationException("Cannot find android sdk. Make sure sdk.dir is defined in local.properties or the environment variable ANDROID_HOME is set.", null)
            }

            def buildPath = "$project.buildDir/retrolambda"
            def jarPath = "$buildPath/$project.android.compileSdkVersion"
            def rt = "$project.retrolambda.jdk/jre/lib/rt.jar"

            def isLibrary = project.plugins.hasPlugin('android-library')

            def variants = (isLibrary ?
                    project.android.libraryVariants :
                    project.android.applicationVariants) + project.android.testVariants

            variants.each { var ->
                if (project.retrolambda.isIncluded(var.name)) {
                    def name = var.name.capitalize()
                    def oldDestDir = var.javaCompile.destinationDir
                    def newDestDir = project.file("$buildPath/$var.name")
                    def classpathFiles =
                            var.javaCompile.classpath + project.files("$buildPath/$var.name")

                    def newJavaCompile = project.task("_$var.javaCompile.name", type: JavaCompile) {
                        conventionMapping.source = { var.javaCompile.source }
                        conventionMapping.classpath = { var.javaCompile.classpath }
                        destinationDir = newDestDir
                        sourceCompatibility = "1.8"
                        targetCompatibility = "1.8"
                        options.encoding = var.javaCompile.options.encoding
                    }

                    def retrolambdaTask = project.task("compileRetrolambda${name}", dependsOn: [newJavaCompile], type: RetrolambdaTask) {
                        inputDir = newDestDir
                        outputDir = oldDestDir
                        classpath = classpathFiles
                        javaVersion = project.retrolambda.javaVersion
                        jvmArgs = project.retrolambda.jvmArgs
                    }

                    newJavaCompile.doFirst {
                        newJavaCompile.classpath += project.files(rt)
                        newJavaCompile.options.compilerArgs = var.javaCompile.options.compilerArgs
                        newJavaCompile.options.bootClasspath = var.javaCompile.options.bootClasspath
                    }

                    retrolambdaTask.doFirst {
                        if (var.javaCompile.options.bootClasspath) {
                            retrolambdaTask.classpath += project.files(var.javaCompile.options.bootClasspath)
                        } else {
                            // If this is null it means the javaCompile task didn't need to run, don't bother running retrolambda either.
                            throw new StopExecutionException()
                        }
                    }

                    var.javaCompile.finalizedBy(retrolambdaTask)
                    
                    // Hack to only delete the compile action and not any doFirst() or doLast()
                    // I hope gradle doesn't change the class name!
                    def taskActions = var.javaCompile.taskActions
                    def taskRemoved = false
                    def beforeActions = []
                    def afterActions = []
                    for (int i = taskActions.size() - 1; i >= 0; i--) {
                        if (taskActions[i].class.name == "org.gradle.api.internal.project.taskfactory.AnnotationProcessingTaskFactory\$IncrementalTaskAction") {
                            taskActions.remove(i)
                            taskRemoved = true
                        } else if (taskRemoved) {
                            beforeActions.add(taskActions[i])
                            taskActions.remove(i)
                        } else {
                            afterActions.add(taskActions[i])
                            taskActions.remove(i)
                        }
                    }
                    
                    if (!taskRemoved) {
                        throw new ProjectConfigurationException("Unable to delete old javaCompile action, maybe the class name has changed? Please submit a bug report with what version of gradle you are using.", null)
                    }
                    
                    // Move any after to the retrolambda task to that they run after retrolambda
                    beforeActions.each {
                        newJavaCompile.doFirst(it)
                    }
                    afterActions.each {
                        retrolambdaTask.doLast(it)
                    }

                    // Ensure retrolamba runs before compiling tests
                    def compileTestTaskName = "compile${var.name.capitalize()}UnitTestJava"
                    def compileTestTask = project.tasks.findByName(compileTestTaskName)
                    if (compileTestTask != null) {
                        compileTestTask.mustRunAfter(retrolambdaTask)
                        // We need to add the rt to the classpath to support lambdas in the tests themselves 
                        compileTestTask.classpath += project.files(rt)
                        
                        if (!project.retrolambda.onJava8) {
                            // Set JDK 8 for the compiler task
                            compileTestTask.doFirst {
                                it.options.fork = true
                                def javac = "${project.retrolambda.tryGetJdk()}/bin/javac"
                                if (!checkIfExecutableExists(javac)) throw new ProjectConfigurationException("Cannot find executable: $javac", null)
                                it.options.forkOptions.executable = javac
                            }
                        }
                    }

                    def extractTaskName = "extract${var.name.capitalize()}Annotations"
                    def extractTask = project.tasks.findByName(extractTaskName)
                    if (extractTask != null) {
                        extractTask.deleteAllActions()
                        project.logger.warn("$extractTaskName is incompatible with java 8 sources and has been disabled.")
                    }

                    if (!project.retrolambda.onJava8) {
                        // Set JDK 8 for compiler task
                        newJavaCompile.doFirst {
                            it.options.fork = true
                            def javac = "${project.retrolambda.tryGetJdk()}/bin/javac"
                            if (!checkIfExecutableExists(javac)) throw new ProjectConfigurationException("Cannot find executable: $javac", null)
                            it.options.forkOptions.executable = javac
                        }
                    }
                }
            }
        }
    }
}
