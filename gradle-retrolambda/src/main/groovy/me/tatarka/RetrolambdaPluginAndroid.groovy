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

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.TestExtension
import com.android.build.gradle.TestPlugin
import com.android.build.gradle.FeatureExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.TestVariant
import com.android.build.gradle.api.UnitTestVariant
import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test

import static me.tatarka.RetrolambdaPlugin.checkIfExecutableExists

@CompileStatic
class RetrolambdaPluginAndroid implements Plugin<Project> {
    @Override
    void apply(Project project) {
        def retrolambda = project.extensions.getByType(RetrolambdaExtension)
        def transform = new RetrolambdaTransform(project, retrolambda)

        if (project.plugins.hasPlugin(LibraryPlugin)) {
            def android = project.extensions.getByType(LibraryExtension)
            android.registerTransform(transform)

            android.libraryVariants.all { BaseVariant variant ->
                configureCompileJavaTask(project, variant, transform)
            }
            android.testVariants.all { TestVariant variant ->
                configureCompileJavaTask(project, variant, transform)
            }
            android.unitTestVariants.all { UnitTestVariant variant ->
                configureUnitTestTask(project, variant.name, variant.javaCompile)
            }
        } else if (project.plugins.hasPlugin(TestPlugin)) {
            def android = project.extensions.getByType(TestExtension)
            android.registerTransform(transform)

            android.applicationVariants.all { BaseVariant variant ->
                configureCompileJavaTask(project, variant, transform)
            }
        } else if (project.plugins.hasPlugin(AppPlugin)) {
            def android = project.extensions.getByType(AppExtension)
            android.registerTransform(transform)

            android.applicationVariants.all { BaseVariant variant ->
                configureCompileJavaTask(project, variant, transform)
            }
            android.testVariants.all { TestVariant variant ->
                configureCompileJavaTask(project, variant, transform)
            }
            android.unitTestVariants.all { UnitTestVariant variant ->
                configureUnitTestTask(project, variant.name, variant.javaCompile)
            }
        } else {
            //the Feature plugin doesn't exist for older versions of AGP, so we need to check
            //it's in the classpath
            try {
                Class.forName( "com.android.build.gradle.FeaturePlugin" )

                def android = project.extensions.getByType(FeatureExtension)
                android.registerTransform(transform)

                android.featureVariants.all { BaseVariant variant ->
                    configureCompileJavaTask(project, variant, transform)
                }
                android.libraryVariants.all { BaseVariant variant ->
                    configureCompileJavaTask(project, variant, transform)
                }
                android.testVariants.all { TestVariant variant ->
                    configureCompileJavaTask(project, variant, transform)
                }
                android.unitTestVariants.all { UnitTestVariant variant ->
                    configureUnitTestTask(project, variant.name, variant.javaCompile)
                }
            } catch( ClassNotFoundException e ) {
                //Feature plugin doesn't exist
            }
        }
    }

    private static configureCompileJavaTask(Project project, BaseVariant variant, RetrolambdaTransform transform) {
        variant.javaCompile.doFirst {
            def retrolambda = project.extensions.getByType(RetrolambdaExtension)
            def rt = "$retrolambda.jdk/jre/lib/rt.jar"

            variant.javaCompile.classpath = variant.javaCompile.classpath + project.files(rt)
            ensureCompileOnJava8(retrolambda, variant.javaCompile)
        }

        transform.putVariant(variant)
    }

    private
    static configureUnitTestTask(Project project, String variant, JavaCompile javaCompileTask) {
        javaCompileTask.doFirst {
            def retrolambda = project.extensions.getByType(RetrolambdaExtension)
            def rt = "$retrolambda.jdk/jre/lib/rt.jar"

            // We need to add the rt to the classpath to support lambdas in the tests themselves
            javaCompileTask.classpath = javaCompileTask.classpath + project.files(rt)

            ensureCompileOnJava8(retrolambda, javaCompileTask)
        }

        Test runTask = (Test) project.tasks.findByName("test${RetrolambdaUtil.capitalize(variant)}UnitTest")
        if (runTask) {
            runTask.doFirst {
                def retrolambda = project.extensions.getByType(RetrolambdaExtension)
                ensureRunOnJava8(retrolambda, runTask)
            }
        }
    }

    private static ensureCompileOnJava8(RetrolambdaExtension retrolambda, JavaCompile javaCompile) {
        javaCompile.sourceCompatibility = "1.8"
        javaCompile.targetCompatibility = "1.8"

        if (!retrolambda.onJava8) {
            // Set JDK 8 for the compiler task
            def javac = "${retrolambda.tryGetJdk()}/bin/javac"
            if (!checkIfExecutableExists(javac)) {
                throw new ProjectConfigurationException("Cannot find executable: $javac", (Throwable) null)
            }
            javaCompile.options.fork = true
            javaCompile.options.forkOptions.executable = javac
        }
    }

    private static ensureRunOnJava8(RetrolambdaExtension retrolambda, Test test) {
        if (!retrolambda.onJava8) {
            def java = "${retrolambda.tryGetJdk()}/bin/java"
            if (!checkIfExecutableExists(java)) {
                throw new ProjectConfigurationException("Cannot find executable: $java", (Throwable) null)
            }
            test.executable java
        }
    }
}
