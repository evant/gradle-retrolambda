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
/**
 * Created with IntelliJ IDEA.
 * User: evan
 * Date: 8/4/13
 * Time: 1:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class RetrolambdaExtension {
    Object compile = "net.orfjackal.retrolambda:retrolambda:1.1.2"
    String jdk = System.getenv("JAVA_HOME")
    int bytecodeVersion = 50
    List<String> excludes = []
    List<String> includes = []
    boolean isOnJava8 = System.properties.'java.version'.startsWith('1.8')

    public void exclude(Object... e) {
        excludes.addAll(e.collect {i -> i.toString()})
    }

    public void include(Object... e) {
        includes.addAll(e.collect {i -> i.toString()})
    }

    public void setCompile(Object c) {
        compile = c
    }

    public void setBytecodeVersion(int v) {
        bytecodeVersion = v
    }

    public void setJavaVersion(JavaVersion v) {
        switch (v.majorVersion) {
            case '6': bytecodeVersion = 50
                break
            case '7': bytecodeVersion = 51
                break
            default:
                throw new RuntimeException("Unknown java version: $v, only 6 or 7 are accepted")
        }
    }

    public void setJdk(String path) {
        jdk = path
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
}
