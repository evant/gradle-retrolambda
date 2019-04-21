Gradle Retrolambda Plugin
========================

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/me.tatarka/gradle-retrolambda/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/me.tatarka/gradle-retrolambda)

This plugin will automatically build your java or *android* project with
retrolambda, giving you lambda goodness on java 6 or 7. It relies on the
wonderful [retrolambda](https://github.com/orfjackal/retrolambda) by Esko
Luontola.

Note: The minimum android gradle plugin is `1.5.0` and the minimum gradle plugin is `2.5`.

Usage
----

1. Download [jdk8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) and set it as your default.

2. Add the following to your build.gradle

   ```groovy
   buildscript {
      repositories {
         mavenCentral()
      }

      dependencies {
         classpath 'me.tatarka:gradle-retrolambda:3.7.1'
      }
   }

   // Required because retrolambda is on maven central
   repositories {
      mavenCentral()
   }

   apply plugin: 'com.android.application' //or apply plugin: 'java'
   apply plugin: 'me.tatarka.retrolambda'
   ```
   alternatively, you can use the new plugin syntax for gradle `2.1+`
   ```groovy
   plugins {
      id "me.tatarka.retrolambda" version "3.7.1"
   }
   ```

3. There is no step three!

The plugin will compile the source code with java8 and then replace the class
files with the output of retrolambda.

Configuration
-------------
You can add a block like the following to configure the plugin:
```groovy
retrolambda {
  javaVersion JavaVersion.VERSION_1_6
  jvmArgs '-arg1', '-arg2'
  defaultMethods false
  incremental true
}
```

- `javaVersion` Set the java version to compile to. The default is 6. Only 5, 6 or 7 are accepted.
- `include 'Debug', 'Release'` Sets which sets/variants to run through
    retrolambda. The default is all of them.
- `exclude 'Test'` Sets which sets/variants to not run through retrolambda. Only
    one of either `include` or `exclude` should be defined.
- `jvmArgs` Add additional jvm args when running retrolambda.
- `defaultMethods` Turn on default and static methods in interfaces support. Note: due to a
   limitation in retrolamba, this will set `incremental` to false. The default is false.
- `incremental` Setting this to false forces all of your class files to be run through retrolambda
   instead of only the ones that have changed. The default is true.

### Using a Different Version of the retrolambda.jar

The default version of retrolambda used is
`'net.orfjackal.retrolambda:retrolambda:2.5.6'`. If you want to use a different
one, you can configure it in your dependencies.

```groovy
dependencies {
  // Latest one on maven central
  retrolambdaConfig 'net.orfjackal.retrolambda:retrolambda:+'
  // Or a local version
  // retrolambdaConfig files('libs/retrolambda.jar')
}
```

### Deprecated Features

If you are running with java 6 or 7 you should really consider updating. However, you may use the
below configuration instead.

Set the environment variable `JAVA8_HOME` to point to the java 8 jdk. Alternatively, you can set the
`jdk` property.
```groovy
retrolambda {
  jdk System.getenv("JAVA8_HOME")
}
```

You can force unit tests to be run with an older version of java by setting
 `JAVA5_HOME`/`JAVA6_HOME`/`JAVA7_HOME` or with the `oldJdk` property.
```groovy
retrolambda {
  oldJdk System.getenv("JAVA6_HOME")
}
```

Android Studio Setup
--------------------
Add these lines to your `build.gradle` to inform the IDE of the language level.

```groovy
android {
  compileOptions {
    sourceCompatibility JavaVersion.VERSION_1_8
    targetCompatibility JavaVersion.VERSION_1_8
  }
}
```

Proguard
----------
This plugin is fully compatible with proguard (since `v2.4.0`). In your proguard file, add
```
-dontwarn java.lang.invoke.*
-dontwarn **$$Lambda$*
```

Known Issues
---------------
### Lint fails on java files that have lambdas.
First try updating to the latest version of the android gradle plugin. Newer versions of lint are
compatible with java 8 sources. If you can't for some reason, you can still use the
[experimental fork](https://github.com/evant/android-retrolambda-lombok) to fix the issue.

### Using Google Play Services causes retrolambda to fail
Version `5.0.77` contains bytecode that is incompatible with retrolambda. This should be fixed in
newer versions of play services, if you can update, that should be the preferred solution. To work
around this issue, you can either use an earlier version like `4.4.52` or add `-noverify` to the jvm
args. See [orfjackal/retrolambda#25](https://github.com/orfjackal/retrolambda/issues/25) for more
information.

```groovy
retrolambda {
  jvmArgs '-noverify'
}
```

Updates
-------
All updates have moved to the [CHANGELOG](https://github.com/evant/gradle-retrolambda/blob/master/CHANGELOG.md).

License
-------

    Copyright 2013 Evan Tatarka
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
       http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
