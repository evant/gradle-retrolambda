package me.tatarka;


import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;

import static me.tatarka.TestHelpers.getPluginClasspath;
import static me.tatarka.TestHelpers.newestSupportedAndroidPluginVersion;
import static me.tatarka.TestHelpers.oldestSupportedAndroidPluginVersion;
import static me.tatarka.TestHelpers.writeFile;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class AndroidAppPluginTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                oldestSupportedAndroidPluginVersion(),
                newestSupportedAndroidPluginVersion()
        });
    }

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder();
    private final String androidVersion;
    private final String gradleVersion;
    private File rootDir;
    private File buildFile;

    public AndroidAppPluginTest(String androidVersion, String gradleVersion) {
        this.androidVersion = androidVersion;
        this.gradleVersion = gradleVersion;
    }

    @Before
    public void setup() throws Exception {
        rootDir = testProjectDir.getRoot();
        buildFile = testProjectDir.newFile("build.gradle");
    }

    @Test
    public void assembleDebug() throws Exception {
        writeFile(buildFile,
                //language="Groovy"
                "buildscript {\n" +
                        "    System.properties['com.android.build.gradle.overrideVersionCheck'] = 'true'\n" +
                        "    \n" +
                        "    repositories {\n" +
                        "        jcenter()\n" +
                        "    }\n" +
                        "    \n" +
                        "    dependencies {\n" +
                        "        classpath files(" + getPluginClasspath() + ")\n" +
                        "        classpath 'com.android.tools.build:gradle:" + androidVersion + "'\n" +
                        "    }\n" +
                        "}\n" +
                        "\n" +
                        "apply plugin: 'com.android.application'\n" +
                        "apply plugin: 'me.tatarka.retrolambda'\n" +
                        "\n" +
                        "repositories {\n" +
                        "    mavenCentral()\n" +
                        "}\n" +
                        "\n" +
                        "android {\n" +
                        "    compileSdkVersion 25\n" +
                        "    buildToolsVersion '25.0.2'\n" +
                        "    \n" +
                        "    defaultConfig {\n" +
                        "        minSdkVersion 15\n" +
                        "        targetSdkVersion 25\n" +
                        "    }\n" +
                        "}");

        File manifestFile = new File(rootDir, "src/main/AndroidManifest.xml");

        writeFile(manifestFile,
                //language="XML"
                "<manifest package=\"test\">\n" +
                        "    <application/>\n" +
                        "</manifest>");

        File javaFile = new File(rootDir, "src/main/java/MainActivity.java");

        writeFile(javaFile, "package test;" +
                "import android.app.Activity;" +
                "import android.os.Bundle;" +
                "import android.util.Log;" +
                "public class MainActivity extends Activity {\n" +
                "    public void onCreate(Bundle savedInstanceState) {\n" +
                "        Runnable lambda = () -> Log.d(\"MainActivity\", \"Hello, Lambda!\");\n" +
                "        lambda.run();\n" +
                "    }\n" +
                "}");

        StringWriter errorOutput = new StringWriter();
        BuildResult result = GradleRunner.create()
                .withGradleVersion(gradleVersion)
                .withProjectDir(rootDir)
                .withArguments("assembleDebug", "--stacktrace")
                .forwardStdError(errorOutput)
                .build();

        assertThat(errorOutput.toString()).isNullOrEmpty();

        File mainClassFile = new File(rootDir, "build/intermediates/transforms/retrolambda/debug/folders/1/1/retrolambda/test/MainActivity.class");
        File lambdaClassFile = new File(rootDir, "build/intermediates/transforms/retrolambda/debug/folders/1/1/retrolambda/test/MainActivity$$Lambda$1.class");

        assertThat(mainClassFile).exists();
        assertThat(lambdaClassFile).exists();
    }

    @Test
    public void assembleDebugIncremental() throws Exception {
        writeFile(buildFile,
                //language="Groovy"
                "buildscript {\n" +
                        "    repositories {\n" +
                        "        jcenter()\n" +
                        "    }\n" +
                        "    \n" +
                        "    dependencies {\n" +
                        "        classpath files(" + getPluginClasspath() + ")\n" +
                        "        classpath 'com.android.tools.build:gradle:" + androidVersion + "'\n" +
                        "    }\n" +
                        "}\n" +
                        "\n" +
                        "apply plugin: 'com.android.application'\n" +
                        "apply plugin: 'me.tatarka.retrolambda'\n" +
                        "\n" +
                        "repositories {\n" +
                        "    mavenCentral()\n" +
                        "}\n" +
                        "\n" +
                        "android {\n" +
                        "    compileSdkVersion 25\n" +
                        "    buildToolsVersion '25.0.2'\n" +
                        "    \n" +
                        "    defaultConfig {\n" +
                        "        minSdkVersion 15\n" +
                        "        targetSdkVersion 25\n" +
                        "    }\n" +
                        "}");

        File manifestFile = new File(rootDir, "src/main/AndroidManifest.xml");

        writeFile(manifestFile,
                //language="XML"
                "<manifest package=\"test\">\n" +
                        "    <application/>\n" +
                        "</manifest>");

        File javaFile = new File(rootDir, "src/main/java/MainActivity.java");

        writeFile(javaFile, "package test;" +
                "import android.app.Activity;" +
                "import android.os.Bundle;" +
                "import android.util.Log;" +
                "public class MainActivity extends Activity {\n" +
                "    public void onCreate(Bundle savedInstanceState) {\n" +
                "        Runnable lambda = () -> Log.d(\"MainActivity\", \"Hello, Lambda!\");\n" +
                "        lambda.run();\n" +
                "    }\n" +
                "}");

        StringWriter errorOutput = new StringWriter();
        BuildResult result = GradleRunner.create()
                .withGradleVersion(gradleVersion)
                .withProjectDir(rootDir)
                .withArguments("assembleDebug", "--stacktrace")
                .forwardStdError(errorOutput)
                .build();

        assertThat(errorOutput.toString()).isNullOrEmpty();

        writeFile(javaFile, "package test;" +
                "import android.app.Activity;" +
                "import android.os.Bundle;" +
                "import android.util.Log;" +
                "public class MainActivity extends Activity {\n" +
                "    public void onCreate(Bundle savedInstanceState) {\n" +
                "        Runnable lambda = () -> Log.d(\"MainActivity\", \"Hello, Lambda2!\");\n" +
                "        lambda.run();\n" +
                "    }\n" +
                "}");

        errorOutput = new StringWriter();
        result = GradleRunner.create()
                .withGradleVersion(gradleVersion)
                .withProjectDir(rootDir)
                .withArguments("assembleDebug", "--stacktrace")
                .forwardStdError(errorOutput)
                .build();

        assertThat(errorOutput.toString()).isNullOrEmpty();

        File mainClassFile = new File(rootDir, "build/intermediates/transforms/retrolambda/debug/folders/1/1/retrolambda/test/MainActivity.class");
        File lambdaClassFile = new File(rootDir, "build/intermediates/transforms/retrolambda/debug/folders/1/1/retrolambda/test/MainActivity$$Lambda$1.class");

        assertThat(mainClassFile).exists();
        assertThat(lambdaClassFile).exists();
    }

    @Test
    public void unitTest() throws Exception {
        writeFile(buildFile,
                //language="Groovy"
                "buildscript {\n" +
                        "    repositories {\n" +
                        "        jcenter()\n" +
                        "    }\n" +
                        "    \n" +
                        "    dependencies {\n" +
                        "        classpath files(" + getPluginClasspath() + ")\n" +
                        "        classpath 'com.android.tools.build:gradle:" + androidVersion + "'\n" +
                        "    }\n" +
                        "}\n" +
                        "\n" +
                        "apply plugin: 'com.android.application'\n" +
                        "apply plugin: 'me.tatarka.retrolambda'\n" +
                        "\n" +
                        "repositories {\n" +
                        "    mavenCentral()\n" +
                        "}\n" +
                        "\n" +
                        "android {\n" +
                        "    compileSdkVersion 25\n" +
                        "    buildToolsVersion '25.0.2'\n" +
                        "    \n" +
                        "    defaultConfig {\n" +
                        "        minSdkVersion 15\n" +
                        "        targetSdkVersion 25\n" +
                        "    }\n" +
                        "}\n" +
                        "\n" +
                        "dependencies {\n" +
                        "    testCompile 'junit:junit:4.12'\n" +
                        "}");

        File manifestFile = new File(rootDir, "src/main/AndroidManifest.xml");

        writeFile(manifestFile,
                //language="XML"
                "<manifest package=\"test\">\n" +
                        "    <application/>\n" +
                        "</manifest>");

        File javaFile = new File(rootDir, "src/main/java/Main.java");

        writeFile(javaFile, "import java.util.concurrent.Callable;\n" +
                "\n" +
                "public class Main {\n" +
                "    public static Callable<String> f() {\n" +
                "        return () -> \"Hello, Lambda Test!\";\n" +
                "    }\n" +
                "}");

        File testJavaFile = new File(rootDir, "src/test/java/Test.java");

        writeFile(testJavaFile, "import org.junit.Assert;\n" +
                "import org.junit.runner.RunWith;\n" +
                "import org.junit.runners.JUnit4;\n" +
                "\n" +
                "import java.util.concurrent.Callable;\n" +
                "\n" +
                "@RunWith(JUnit4.class)\n" +
                "public class Test {\n" +
                "    @org.junit.Test\n" +
                "    public void test() throws Exception {\n" +
                "        Runnable lambda = () -> Assert.assertTrue(true);\n" +
                "        lambda.run();\n" +
                "        Assert.assertEquals(\"Hello, Lambda Test!\", Main.f().call());\n" +
                "    }\n" +
                "}");

        StringWriter errorOutput = new StringWriter();
        BuildResult result = GradleRunner.create()
                .withGradleVersion(gradleVersion)
                .withProjectDir(rootDir)
                .withArguments("test", "--stacktrace")
                .forwardStdError(errorOutput)
                .build();

        assertThat(errorOutput.toString()).isNullOrEmpty();
    }

    @Test
    public void androidTest() throws Exception {
        writeFile(buildFile,
                //language="Groovy"
                "buildscript {\n" +
                        "    repositories {\n" +
                        "        jcenter()\n" +
                        "    }\n" +
                        "    \n" +
                        "    dependencies {\n" +
                        "        classpath files(" + getPluginClasspath() + ")\n" +
                        "        classpath 'com.android.tools.build:gradle:" + androidVersion + "'\n" +
                        "    }\n" +
                        "}\n" +
                        "\n" +
                        "apply plugin: 'com.android.application'\n" +
                        "apply plugin: 'me.tatarka.retrolambda'\n" +
                        "\n" +
                        "repositories {\n" +
                        "    mavenCentral()\n" +
                        "}\n" +
                        "\n" +
                        "android {\n" +
                        "    compileSdkVersion 25\n" +
                        "    buildToolsVersion '25.0.2'\n" +
                        "    \n" +
                        "    defaultConfig {\n" +
                        "        minSdkVersion 15\n" +
                        "        targetSdkVersion 25\n" +
                        "        testInstrumentationRunner \"android.support.test.runner.AndroidJUnitRunner\"\n" +
                        "    }\n" +
                        "}\n" +
                        "\n" +
                        "dependencies {\n" +
                        "    androidTestCompile 'com.android.support.test:runner:0.4'\n" +
                        "    androidTestCompile 'com.android.support.test:rules:0.4'\n" +
                        "}");

        File manifestFile = new File(rootDir, "src/main/AndroidManifest.xml");

        writeFile(manifestFile,
                //language="XML"
                "<manifest package=\"com.example.test\">\n" +
                        "    <application/>\n" +
                        "</manifest>");

        File javaFile = new File(rootDir, "src/main/java/Main.java");

        writeFile(javaFile, "import java.util.concurrent.Callable;\n" +
                "\n" +
                "public class Main {\n" +
                "    public static Callable<String> f() {\n" +
                "        return () -> \"Hello, Lambda Test!\";\n" +
                "    }\n" +
                "}");

        File testJavaFile = new File(rootDir, "src/androidTest/java/Test.java");

        writeFile(testJavaFile, "import org.junit.Assert;\n" +
                "import org.junit.runner.RunWith;\n" +
                "import android.support.test.runner.AndroidJUnit4;\n" +
                "\n" +
                "import java.util.concurrent.Callable;\n" +
                "\n" +
                "@RunWith(AndroidJUnit4.class)\n" +
                "public class Test {\n" +
                "    @org.junit.Test\n" +
                "    public void test() throws Exception {\n" +
                "        Runnable lambda = () -> Assert.assertTrue(true);\n" +
                "        lambda.run();\n" +
                "        Assert.assertEquals(\"Hello, Lambda Test!\", Main.f().call());\n" +
                "    }\n" +
                "}");

        StringWriter errorOutput = new StringWriter();
        BuildResult result = GradleRunner.create()
                .withGradleVersion(gradleVersion)
                .withProjectDir(rootDir)
                .withArguments("connectedCheck", "--stacktrace")
                .forwardStdError(errorOutput)
                .build();
        
        assertThat(errorOutput.toString()).isNullOrEmpty();
    }

    @Test
    public void withKotlin() throws Exception {
        writeFile(buildFile,
                //language="Groovy"
                "buildscript {\n" +
                        "    repositories {\n" +
                        "        jcenter()\n" +
                        "    }\n" +
                        "    \n" +
                        "    dependencies {\n" +
                        "        classpath files(" + getPluginClasspath() + ")\n" +
                        "        classpath 'com.android.tools.build:gradle:" + androidVersion + "'\n" +
                        "        classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:1.0.4'" +
                        "    }\n" +
                        "}\n" +
                        "\n" +
                        "apply plugin: 'com.android.application'\n" +
                        "apply plugin: 'me.tatarka.retrolambda'\n" +
                        "apply plugin: 'kotlin-android'\n" +
                        "\n" +
                        "repositories {\n" +
                        "    mavenCentral()\n" +
                        "}\n" +
                        "\n" +
                        "android {\n" +
                        "    compileSdkVersion 25\n" +
                        "    buildToolsVersion '25.0.2'\n" +
                        "    \n" +
                        "    defaultConfig {\n" +
                        "        minSdkVersion 15\n" +
                        "        targetSdkVersion 25\n" +
                        "    }\n" +
                        "}\n" +
                        "dependencies {\n" +
                        "   compile 'org.jetbrains.kotlin:kotlin-stdlib:1.0.4'\n" +
                        "}");

        File manifestFile = new File(rootDir, "src/main/AndroidManifest.xml");

        writeFile(manifestFile,
                //language="XML"
                "<manifest package=\"test\">\n" +
                        "    <application/>\n" +
                        "</manifest>");

        File javaFile1 = new File(rootDir, "src/main/java/test/MainActivity.java");
        File javaFile2 = new File(rootDir, "src/main/java/test/JavaFile.java");

        File kotlinFile = new File(rootDir, "src/main/kotlin/test/KotlinFile.kt");

        writeFile(javaFile1, "package test;" +
                "import android.app.Activity;" +
                "import android.os.Bundle;" +
                "import android.util.Log;" +
                "public class MainActivity extends Activity {\n" +
                "    public void onCreate(Bundle savedInstanceState) {\n" +
                "        Runnable lambda = () -> Log.d(\"MainActivity\", \"Hello, Lambda!\");\n" +
                "        lambda.run();\n" +
                "        new KotlinFile().doThis();" +
                "    }\n" +
                "}");

        writeFile(javaFile2, "package test;" +
                "public class JavaFile {\n" +
                "   public void doThat() {\n" +
                "       System.out.println(\"That\");" +
                "   }" +
                "}");

        writeFile(kotlinFile, "package test\n" +
                "import kotlin.io.println\n" +
                "class KotlinFile {\n" +
                "public fun doThis() {\n" +
                "   println(\"This\")\n" +
                "   JavaFile().doThat()\n" +
                "}\n" +
                "}");

        StringWriter errorOutput = new StringWriter();
        BuildResult result = GradleRunner.create()
                .withGradleVersion(gradleVersion)
                .withProjectDir(rootDir)
                .withArguments("assembleDebug", "--stacktrace")
                .forwardStdError(errorOutput)
                .build();

        assertThat(errorOutput.toString()).isNullOrEmpty();

        File mainClassFile = new File(rootDir, "build/intermediates/transforms/retrolambda/debug/folders/1/1/retrolambda/test/MainActivity.class");
        File lambdaClassFile = new File(rootDir, "build/intermediates/transforms/retrolambda/debug/folders/1/1/retrolambda/test/MainActivity$$Lambda$1.class");

        assertThat(mainClassFile).exists();
        assertThat(lambdaClassFile).exists();
    }
}
