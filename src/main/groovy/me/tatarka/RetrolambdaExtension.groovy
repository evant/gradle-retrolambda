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

import org.gradle.api.JavaVersion
import org.gradle.api.ProjectConfigurationException

import java.util.regex.Pattern

import static me.tatarka.RetrolambdaPlugin.javaVersionToBytecode

/**
 * Created with IntelliJ IDEA.
 * User: evan
 * Date: 8/4/13
 * Time: 1:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class RetrolambdaExtension {
    int bytecodeVersion = 50
    List<String> excludes = []
    List<String> includes = []
    String javaHomeProp = System.properties.'java.home'.toString().split(Pattern.quote(System.properties.'file.separator'.toString() + "jre"))[0]
    boolean isOnJava8 = System.properties.'java.version'.startsWith('1.8')

    private String jdk = null
    private String oldJdk = null
    private boolean oldJdkSet = false

    public RetrolambdaExtension() {
        jdk = findJdk()
        oldJdk = findOldJdk()
    }

    public void exclude(Object... e) {
        excludes.addAll(e.collect { i -> i.toString() })
    }

    public void include(Object... e) {
        includes.addAll(e.collect { i -> i.toString() })
    }

    public void setBytecodeVersion(int v) {
        bytecodeVersion = v
        if (!oldJdkSet) oldJdk = findOldJdk()
    }

    public void setJavaVersion(JavaVersion v) {
        bytecodeVersion = javaVersionToBytecode(v)
        if (!oldJdkSet) oldJdk = findOldJdk()
    }

    public JavaVersion getJavaVersion() {
        switch (bytecodeVersion) {
            case 50: return JavaVersion.VERSION_1_6
            case 51: return JavaVersion.VERSION_1_7
        }
    }

    public void setJdk(String path) {
        jdk = path
    }

    public String getJdk() {
        return jdk
    }

    String tryGetJdk() {
        if (jdk == null) {
            throw new ProjectConfigurationException("When running gradle with java 6 or 7, you must set the path to jdk8, either with property retrolambda.jdk or environment variable JAVA8_HOME", null)
        }
        return jdk
    }

    public void setOldJdk(String path) {
        oldJdk = path
        oldJdkSet = true
    }

    public String getOldJdk() {
        return oldJdk
    }

    String tryGetOldJdk() {
        if (oldJdk == null) {
            throw new ProjectConfigurationException("When running gradle with java 8, you must set the path to the old jdk, either with property retrolambda.oldJdk or environment variable JAVA6_HOME/JAVA7_HOME", null)
        }
        return oldJdk
    }

    public boolean isIncluded(String name) {
        if (includes.isEmpty() && excludes.isEmpty()) return true
        if (excludes.isEmpty() && excludes.contains(name)) return false;
        if (includes.isEmpty() && !includes.contains(name)) return false;
        return true
    }

    public boolean isOnJava8() {
        return isOnJava8;
    }

    private String findJdk() {
        if (isOnJava8) {
            if (javaHomeProp) {
                return javaHomeProp
            } else {
                return System.getenv("JAVA_HOME")
            }
        } else {
            return System.getenv("JAVA8_HOME")
        }
    }

    private String findOldJdk() {
        if (!isOnJava8) {
            return System.getenv("JAVA_HOME")
        } else {
            switch (bytecodeVersion) {
                case 50: return System.getenv("JAVA6_HOME")
                case 51: return System.getenv("JAVA7_HOME")
            }
            return null
        }
    }
}
