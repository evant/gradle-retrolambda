Gradle Retrolambda Plugin
========================

This plugin will automatically build your java or *android* project with
retrolambda, giving you lambda goodness on java 6 or 7. It relies on the
wonderful [retrolambda](https://github.com/orfjackal/retrolambda) by Esko
Luontola.

Note: The minimum android gradle plugin is `0.8+`.

Usage
----

1. Download [jdk8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html).

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
         classpath 'me.tatarka:gradle-retrolambda:2.4-SNAPSHOT'
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

Configuration
-------------

Configuration is entirely optional, the plugin will by default pick up the
`JAVA6_HOME`/`JAVA7_HOME`/`JAVA8_HOME` environment variables. It's also smart
enough to figure out what version of java you are running gradle with. For
example, if you have java8 set as your default, you only need to define
`JAVA6_HOME`/`JAVA7_HOME`. If you need to though, you can add a block like the
following to configure the plugin:

```groovy
retrolambda {
  jdk System.getenv("JAVA8_HOME")
  oldJdk System.getenv("JAVA6_HOME")
  javaVersion JavaVersion.VERSION_1_6
  jvmArgs '-arg1', '-arg2'
}
```

- `jdk` Set the path to the java 8 jdk. The default is found using the
    environment variable `JAVA8_HOME`. If you a running gradle with java 6 or 7,
    you must have either `JAVA8_HOME` or this property set.
- `oldJdk` Sets the path to the java 6 or 7 jdk. The default is found using the
    environment variable `JAVA6_HOME`/`JAVA7_HOME`. If you are running gradle
    with java 8 and wish to run unit tests, you must have either
    `JAVA6_HOME`/`JAVA7_HOME` or this property set. This is so the tests can be
    run with the correct java version.
- `javaVersion` Set the java version to compile to. The default is 6. Only 6 or
    7 are accepted.
- `include 'Debug', 'Release'` Sets which sets/variants to run through
    retrolambda. The default is all of them.
- `exclude 'Test'` Sets which sets/variants to not run through retrolambda. Only
    one of either `include` or `exclude` should be defined.
- `jvmArgs` Add additional jvm args when running retrolambda.

### Using a Different Version of the retrolambda.jar

The default version of retrolambda used is
`'net.orfjackal.retrolambda:retrolambda:1.6.0'`. If you want to use a different
one, you can configure it in your dependencies.

```groovy
dependencies {
  // Latest one on maven central
  retrolambdaConfig 'net.orfjackal.retrolambda:retrolambda:1.+'
  // Or a local version
  // retrolambdaConfig files('libs/retrolambda.jar')
}
```

Android Studio Setup
--------------------
Luckily Android Studio already has built-in lambda support! Enable it for your
android project by going to `File -> Project Structure -> Project` and selecting
`8.0 - Lambdas, type annotations etc.` under `Project language level`.

You should also add these lines to you `build.gradle` so it doesn't try to
change the language level on you when you refresh.

```groovy
android {
  compileOptions {
    sourceCompatibility JavaVersion.VERSION_1_8
    targetCompatibility JavaVersion.VERSION_1_8
  }
}
```

Known Issues
---------------
### Using Google Play Services causes retrolambda to fail
Version `5.0.77` contains bytecode that is incompatible with retrolambda. To
work around this issue, you can either use an earlier version like `4.4.52` or
add `-noverify` to the jvm args. See
[orfjackal/retrolambda#25](https://github.com/orfjackal/retrolambda/issues/25)
for more information.

```groovy
retrolambda {
  jvmArgs '-noverify'
}
```

### Compiling for android-L doesn't work when using Android Studio's sdk manager. 
For some reason only known to the gods, when using Android Studio's sdk manager,
there is no `android-L` directory sdk directory. Instead, it happily builds
using the `android-20` directory instead. To work around this, you can symlink
the `android-L` directory to point to `android-20`. See
[#36](https://github.com/evant/gradle-retrolambda/issues/36).

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

### 2.4.0

- Better incremental compile method that doesn't break lint and proguard (and
probably other tasks). Because of this, `retrolambda.incremental` is deprecated
and does nothing.
- Better handling of manually setting the retrolamba version with
`retrolambConfig`.
- Don't use the retrolambda javaagent if using version `1.6.0+`.
- Set the default retrolambda version to `1.6.0`.

#### 2.3.1

- Fixed `retrolambda.incremental false` causing the retrolambda task not to run.

#### 2.3.0

- Add ability to set `retrolambda.incremental false` to disable incremental compilation, since it is
incompatible with android lint/proguard.

#### 2.2.3

- Change dependency back to `localGroovy()`, `org.codehaus.groovy:groovy-all:2.3.3` was causing 
issues.

#### 2.2.2

- Support a `java.home` path that does not end in `/jre`, by using it as it is.
This is an issue on OSX which may have a different directory structure. (loune)

#### 2.2.1

- Ensure output directory is created even if the source set is missing files for the java plugin.
Otherwise, compiling the source set would error out.

#### 2.2.0

- Added way to add custom jvm arguments when running retrolambda.
- Disable `extractAnnotations` tasks since they are incompatible with java 8 sources.

Older updates have moved to the [CHANGELOG](https://github.com/evant/gradle-retrolambda/blob/master/CHANGELOG.md).
