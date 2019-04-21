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

import groovy.transform.CompileStatic
import org.gradle.api.JavaVersion
import org.gradle.api.Nullable
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException

import static me.tatarka.RetrolambdaPlugin.javaVersionToBytecode

@CompileStatic
public class RetrolambdaExtension {
    int bytecodeVersion = 50
    List<String> excludes = []
    List<String> includes = []
    List<String> jvmArgs = []
    boolean incremental = true
    boolean defaultMethods = false
    boolean isOnJava8 = JavaVersion.current().java8Compatible

    private Project project
    private String jdk = null
    private String oldJdk = null
    private boolean jdkSet = false
    private boolean oldJdkSet = false

    public RetrolambdaExtension(Project project) {
        this.project = project
    }

    public void exclude(Object... e) {
        excludes.addAll(e.collect { i -> i.toString() })
    }

    public void include(Object... e) {
        includes.addAll(e.collect { i -> i.toString() })
    }

    public void jvmArgs(String... args) {
        jvmArgs.addAll(args)
    }
    
    public void incremental(boolean value) {
        incremental = value
    }
    
    public void defaultMethods(boolean value) {
        defaultMethods = value
    }
    
    public boolean isIncremental() {
        return incremental && !defaultMethods
    }

    public void setBytecodeVersion(int v) {
        bytecodeVersion = v
    }

    public void setJavaVersion(JavaVersion v) {
        bytecodeVersion = javaVersionToBytecode(v)
    }

    public JavaVersion getJavaVersion() {
        switch (bytecodeVersion) {
            case 49: return JavaVersion.VERSION_1_5
            case 50: return JavaVersion.VERSION_1_6
            case 51: return JavaVersion.VERSION_1_7
        }
        throw new AssertionError()
    }

    public void setJdk(String path) {
        jdk = path
        jdkSet = true
    }

    public String getJdk() {
        if (!jdkSet) {
            jdk = findJdk()
            jdkSet = true
        }
        return jdk
    }

    String tryGetJdk() {
        String jdk = getJdk()
        if (jdk == null) {
            throw new ProjectConfigurationException("When running gradle with java 5, 6 or 7, you must set the path to jdk8, either with property retrolambda.jdk or environment variable JAVA8_HOME", (Throwable) null)
        }
        return jdk
    }

    @Deprecated
    public void setOldJdk(String path) {
        oldJdk = path
        oldJdkSet = true
    }

    @Nullable
    @Deprecated
    public String getOldJdk() {
        if (!oldJdkSet) {
            oldJdk = findOldJdk()
            oldJdkSet = true
        }
        if (oldJdk != null) {
            project.logger.warn("running unit tests with an old jdk is deprecated an will be removed in a later version.")
        }
        return oldJdk
    }

    public boolean isIncluded(String name) {
        if (includes.isEmpty() && excludes.isEmpty()) return true
        if (excludes.isEmpty() && !includes.contains(name)) return false;
        if (includes.isEmpty() && excludes.contains(name)) return false;
        return true
    }

    public boolean isOnJava8() {
        return isOnJava8;
    }
    
    private String findJdk() {
        String jdk
        if (isOnJava8) {
            jdk = findCurrentJdk()
        } else {
            jdk = System.getenv("JAVA8_HOME")
        }
        project.logger.info("Retrolambda $project.path found jdk: $jdk")
        return jdk
    }

    private String findOldJdk() {
        String oldJdk
        if (!isOnJava8) {
            oldJdk = findCurrentJdk()
        } else {
            switch (bytecodeVersion) {
                case 49: 
                    oldJdk = System.getenv("JAVA5_HOME")
                    break
                case 50: 
                    oldJdk = System.getenv("JAVA6_HOME")
                    break
                case 51: 
                    oldJdk = System.getenv("JAVA7_HOME")
                    break
                default:
                    oldJdk = null
            }
        }
        project.logger.info("Retrolambda $project.path found oldJdk: $oldJdk")
        return oldJdk
    }

    private static String findCurrentJdk() {
        String javaHomeProp = System.properties.'java.home'
        if (javaHomeProp) {
            int jreIndex = javaHomeProp.lastIndexOf("${File.separator}jre")
            if (jreIndex != -1) {
                return javaHomeProp.substring(0, jreIndex)
            } else {
                return javaHomeProp
            }
        } else {
            return System.getenv("JAVA_HOME")
        }
    }
}
