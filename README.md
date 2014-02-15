Gradle Retrolambda Plugin
========================

This plugin will automatically build your java or *android* project with
retrolambda, giving you lambda goodness on java 6 or 7. It relies on the
wonderful [retrolambda](https://github.com/orfjackal/retrolambda) by Esko
Luontola.

Usage
----

1. Download openjdk8 early access from https://jdk8.java.net/download.html

2. Add the following to your build.gradle

   ```groovy
   buildscript {
      repositories {
         mavenCentral()
      }

      dependencies {
         classpath 'me.tatarka:gradle-retrolambda:1.2.0'
      }
   }

   // Required because retrolambda is on maven central
   repositories {
      mavenCentral()
   }

   apply plugin: 'android' //or apply plugin: 'java'
   apply plugin: 'retrolambda'
   ```
3. There is no step three!

The plugin will compile the source code with java8 and then replace the class
files with the output of retrolambda.

Configuation
------------

You can add a block like the following to configure the plugin:

```groovy
retrolambda {
  compile "net.orfjackal.retrolambda:retrolambda:1.1.2"
  jdk System.getenv("JAVA8_HOME")
  oldJdk System.getenv("JAVA6_HOME")
  javaVersion JavaVersion.VERSION_1_6
}
```

- `compile` Set the path to retrolambda.jar. The default is the one on maven
  central.
- `jdk` Set the path to the java 8 jdk. The default is found using either
  `JAVA8_HOME`. If you a running gradle with java 6 or 7, you must have either
  `JAVA8_HOME` or this property set.
- `oldJdk` Sets the path to the java 6 or 7 jdk. The default is found using
  `JAVA6_HOME`/`JAVA7_HOME`. If you are running gradle with java 8 and wish
  to run unit tests, you must have either `JAVA6_HOME`/`JAVA7_HOME` or this
  property set. This is so the tests can be run with the correct java version.
- `javaVersion` Set the java version to compile to. The default is 6. Only 6 or
  7 are accepted.
- `include 'Debug', 'Release'` Sets which sets/variants to run through
  retrolambda. The default is all of them.
- `exclude 'Test'` Sets which sets/variants to not run through retrolambda. Only
  one of either `include` or `exclude` should be defined.

Android Studio Setup
--------------------
Luckily Android Studio already has built-in lambda support! Enable it for your
android project by going to `File -> Project Structure -> Project` and selecting
`8.0 - Lambdas, type annotations etc.` under `Project language level`.

You should also add these lines to you `build.gradle` so it doesn't try to change
the language level on you when you refresh.

```groovy
android {
  compileOptions {
    sourceCompatibility JavaVersion.VERSION_1_8
    targetCompatibility JavaVersion.VERSION_1_8
  }
}
```

What Black Magic did you use to get this to work on Android?
------------------------------------------------------------

There were two hurdles to overcome when compiling for android. The gradle
android plugin forces a compile targeting java 6 and uses a custom
bootclasspath that doesn't include necessary java8 files. To overcome this, the
plugin:

1. Overrides `-source` and `-target` with 8.
2. Extracts the necessary files out of the java runtime (rt.jar), and patches
android.jar with them.
3. Sets `-bootclasspath` to point to the patched android.jar

Updates
-------

### 1.1
- Fixed bug where java unit tests were not being run through retrolambda
- Allow gradle to be called with java 6 or 7, i.e. Java 8 no longer has to be 
  your default java.
- Thank you Mart-Bogdan for starting these fixes

### 1.1.1
- Fixed not correctly finding java 8 executable when running from java 6 or 7 on
  windows. (Mart-Bogdan)

### 1.2.0
- Support android-library projects
