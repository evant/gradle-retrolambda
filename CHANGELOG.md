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
This is an issue on OSX which may have a different directory structure.

#### 2.2.1

- Ensure output directory is created even if the source set is missing files for the java plugin.
Otherwise, compiling the source set would error out.

#### 2.2.0

- Added way to add custom jvm arguments when running retrolambda.
- Disable `extractAnnotations` tasks since they are incompatible with java 8 sources.

#### 2.1.0

- Also check system property 'java.home' for the current java location. IDEs set this but not
JAVA_HOME, so checking here first is more robust. (aphexcx)

#### 2.0.0
- Hooks into gradle's incremental compilcation support. This should mean faster build times and less
  inconsistencies when changing the build script without running `clean`. To fully take advantage of
  this you need to use retrolambda `1.4.0+` which is now the default.

#### 1.3.3
- Allow `retrolamba` plugin to be applied before or after `java` and `android`
  plugins

#### 1.3.2
- Fixed for android gradle plugin `0.10.+`

#### 1.3.1
- Removed `compile` property, which didn't work anyway. Use `retrolambdaConfig`
  instead.
- Minor error message improvement.

#### 1.3.0
- Support android instrument tests.

#### 1.2.0
- Support android-library projects.

#### 1.1.1
- Fixed not correctly finding java 8 executable when running from java 6 or 7 on
  windows. (Mart-Bogdan)

#### 1.1
- Fixed bug where java unit tests were not being run through retrolambda
- Allow gradle to be called with java 6 or 7, i.e. Java 8 no longer has to be
  your default java.
- Thank you Mart-Bogdan for starting these fixes.
