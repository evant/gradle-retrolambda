package me.tatarka;


import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
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

import static me.tatarka.TestHelpers.*;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class AndroidTestPluginTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                //oldestSupportedAndroidPluginVersion(),
                newestSupportedAndroidPluginVersion()
        });
    }

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder();
    private final String androidVersion;
    private final String gradleVersion;
    private final String buildToolsVersion;
    private File rootDir;

    public AndroidTestPluginTest(String androidVersion, String gradleVersion, String buildToolsVersion, String kotlinVersion) {
        this.androidVersion = androidVersion;
        this.gradleVersion = gradleVersion;
        this.buildToolsVersion = buildToolsVersion;
    }

    @Before
    public void setup() throws Exception {
        rootDir = testProjectDir.getRoot();
        testProjectDir.newFile("build.gradle");
        copyLocalPropertiesIfExists(rootDir);
    }

    @Test
    public void test() throws Exception {
        File settingsFile = testProjectDir.newFile("settings.gradle");
        writeFile(settingsFile,
                //language="Groovy"
                "include \":app\", \":test\"");

        File appRootDir = testProjectDir.newFolder("app");
        writeFile(new File(appRootDir, "build.gradle"),
                //language="Groovy"
                "buildscript {\n" +
                        "    System.properties['com.android.build.gradle.overrideVersionCheck'] = 'true'\n" +
                        "    \n" +
                        "    repositories {\n" +
                        "        maven { url 'https://maven.google.com' }\n" +
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
                        "    maven { url 'https://maven.google.com' }\n" +
                        "    mavenCentral()\n" +
                        "}\n" +
                        "\n" +
                        "android {\n" +
                        "    compileSdkVersion 24\n" +
                        "    buildToolsVersion '" + buildToolsVersion + "'\n" +
                        "    \n" +
                        "    defaultConfig {\n" +
                        "        applicationId \"test.app\"\n" +
                        "        minSdkVersion 15\n" +
                        "        targetSdkVersion 24\n" +
                        "    }\n" +
                        "    \n" +
                        "    publishNonDefault true\n" +
                        "}");

        File appManifestFile = new File(appRootDir, "src/main/AndroidManifest.xml");

        writeFile(appManifestFile,
                //language="XML"
                "<manifest package=\"test.app\" " +
                            "xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
                        "    <application/>\n" +
                        "</manifest>");

        File javaFile = new File(appRootDir, "src/main/java/Main.java");

        writeFile(javaFile, "import java.util.concurrent.Callable;\n" +
                "\n" +
                "public class Main {\n" +
                "    public static Callable<String> f() {\n" +
                "        return () -> \"Hello, Lambda Test!\";\n" +
                "    }\n" +
                "}");

        File testRootDir = testProjectDir.newFolder("test");
        writeFile(new File(testRootDir, "build.gradle"),
                //language="Groovy"
                "buildscript {\n" +
                        "    repositories {\n" +
                        "        maven { url 'https://maven.google.com' }\n" +
                        "        jcenter()\n" +
                        "    }\n" +
                        "    \n" +
                        "    dependencies {\n" +
                        "        classpath files(" + getPluginClasspath() + ")\n" +
                        "        classpath 'com.android.tools.build:gradle:" + androidVersion + "'\n" +
                        "    }\n" +
                        "}\n" +
                        "\n" +
                        "apply plugin: 'com.android.test'\n" +
                        "apply plugin: 'me.tatarka.retrolambda'\n" +
                        "\n" +
                        "repositories {\n" +
                        "    maven { url 'https://maven.google.com' }\n" +
                        "    mavenCentral()\n" +
                        "}\n" +
                        "\n" +
                        "android {\n" +
                        "    compileSdkVersion 23\n" +
                        "    buildToolsVersion '" + buildToolsVersion + "'\n" +
                        "    \n" +
                        "    targetProjectPath ':app'\n" +
                        "    targetVariant 'debug'\n" +
                        "    \n" +
                        "    defaultConfig {\n" +
                        "        minSdkVersion 15\n" +
                        "        targetSdkVersion 24\n" +
                        "        testInstrumentationRunner \"android.support.test.runner.AndroidJUnitRunner\"\n" +
                        "    }\n" +
                        "}\n" +
                        "\n" +
                        "dependencies {\n" +
                        "    compile 'com.android.support.test:runner:0.4'\n" +
                        "    compile 'com.android.support.test:rules:0.4'\n" +
                        "}");

        File testManifestFile = new File(testRootDir, "src/main/AndroidManifest.xml");

        writeFile(testManifestFile,
                //language="XML"
                "<manifest package=\"test.test\" " +
                            "xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
                        "    <application/>\n" +
                        "</manifest>");

        File testJavaFile = new File(testRootDir, "src/main/java/Test.java");

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

        assertThat(result.task(":test:connectedCheck").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    }
}
