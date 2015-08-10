package me.tatarka

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static org.junit.Assert.assertNotNull

class RetrolambdaAndroidPluginTest {
    @Test
    void testCreateTask() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'com.android.application'
        project.apply plugin: 'me.tatarka.retrolambda'
        project.android {
            compileSdkVersion 19
            buildToolsVersion "19.1"

            defaultConfig {
                minSdkVersion 14
                targetSdkVersion 19
                versionCode 1
                versionName "1.0"
            }
        }
        project.evaluate()
        assertNotNull project.tasks.findByName('compileRetrolambdaDebug')
    }
    
    @Test
    void testCreateBuildTypeTask() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'com.android.application'
        project.apply plugin: 'me.tatarka.retrolambda'
        project.android {
            compileSdkVersion 19
            buildToolsVersion "19.1"

            defaultConfig {
                minSdkVersion 14
                targetSdkVersion 19
                versionCode 1
                versionName "1.0"
            }
            
            buildTypes {
                other
            }
        }
        project.evaluate()
        assertNotNull project.tasks.findByName('compileRetrolambdaOther')
    }

    @Test
    void testCreateFlavorTask() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'com.android.application'
        project.apply plugin: 'me.tatarka.retrolambda'
        project.android {
            compileSdkVersion 19
            buildToolsVersion "19.1"

            defaultConfig {
                minSdkVersion 14
                targetSdkVersion 19
                versionCode 1
                versionName "1.0"
            }

            productFlavors {
               other 
            }
        }
        project.evaluate()
        assertNotNull project.tasks.findByName('compileRetrolambdaOtherDebug')
    }
    
    @Test
    void testAptCompatibility() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'com.android.application'
        project.apply plugin: 'com.neenbedankt.android-apt'
        project.apply plugin: 'me.tatarka.retrolambda'
        project.android {
            compileSdkVersion 19
            buildToolsVersion "19.1"

            defaultConfig {
                minSdkVersion 14
                targetSdkVersion 19
                versionCode 1
                versionName "1.0"
            }
        }
        project.repositories {
            mavenCentral()
        }
        project.dependencies {
            apt 'com.google.dagger:dagger-compiler:2.0'
        }
        project.evaluate()
        //TODO: what exactly should I be looking for?
    }
}