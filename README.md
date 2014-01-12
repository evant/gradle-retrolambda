Gradle Retrolamba Plugin
========================

This plugin will automatically build your java or *android* project with
retrolamba, giving you lambda goodness on java 6 or 7. It relies on the
wonderful [retrolambda](https://github.com/orfjackal/retrolambda) by Esko
Luontola.

Usage
----

1. Download openjdk8 early access with lambda support from
   https://jdk8.java.net/lambda/

2. Add the following to your build.gradle

   ```groovy
   buildscript {
      repositories {
         mavenCentral()

         maven {
            url "https://oss.sonatype.org/content/repositories/snapshots"
         }
      }

      dependencies {
         classpath 'me.tatarka:gradle-retrolambda:1.0-SNAPSHOT'
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
  jdk System.getenv("JAVA_HOME")
  javaVersion JavaVersion.VERSION_1_6
}
```

- `compile` Set the path to retrolambda.jar. The default is the one on maven
  central.
- `jdk` Set the path to the java8 jdk. The default is found using `JAVA_HOME`.
  This setting is only used for finding the java runtime for android, not for
  running the java compiler.
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

There were two hurdles to overcome when compileing for android. The gradle
android plugin forces a compile targeting java 6 and uses a custom
bootclasspath that doesn't include necessary java8 files. To overcome this, the
plugin:

1. Overrides `-source` and `-target` with 8.
2. Extracts the necessary files out of the java runtime (rt.jar), and patches
android.jar with them.
3. Sets `-bootclasspath` to point to the patched android.jar
