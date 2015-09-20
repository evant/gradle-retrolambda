package me.tatarka;


import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.IOException;

import static me.tatarka.TestHelpers.writeBuildFile;
import static me.tatarka.TestHelpers.writeFile;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class RetrolambdaAndroidAppPluginTest {
    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder();
    private File buildFile;
    private File mainFolder;
    private File javaFolder;
    private File buildFolder;

    @Before
    public void setup() throws Exception {
        buildFile = testProjectDir.newFile("build.gradle");
        mainFolder = new File(testProjectDir.getRoot(), "src/main");
        javaFolder = new File(mainFolder, "java");
        if (!javaFolder.mkdirs()) {
            throw new IOException();
        }
        buildFolder = new File(testProjectDir.getRoot(), "build");
    }

    @Test
    public void testAssemble() throws Exception {
        writeBuildFile(buildFile,
                //language="Groovy"
                "buildscript {\n" +
                        "    repositories {\n" +
                        "        jcenter()\n" +
                        "    }\n" +
                        "    \n" +
                        "    dependencies {\n" +
                        "        classpath files($pluginClasspath)\n" +
                        "        classpath 'com.android.tools.build:gradle:1.4.0-beta2'\n" +
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
                        "    compileSdkVersion 23\n" +
                        "    buildToolsVersion '23.0.1'\n" +
                        "    \n" +
                        "    defaultConfig {\n" +
                        "        minSdkVersion 15\n" +
                        "        targetSdkVersion 23\n" +
                        "    }\n" +
                        "}");

        File manifestFile = new File(mainFolder, "AndroidManifest.xml");

        writeFile(manifestFile,
                //language="XML"
                "<manifest package=\"me.tatarka.retrolambda\">\n" +
                "    <application/>\n" +
                "</manifest>");

        File javaFile = new File(javaFolder, "me/tatarka/retrolambda/MainActivity.java");
        javaFile.getParentFile().mkdirs();

        writeFile(javaFile,
                //language="Java"
                "package me.tatarka.retrolambda;" +
                "import android.app.Activity;" +
                "import android.os.Bundle;" +
                "import android.util.Log;" +
                "public class MainActivity extends Activity {\n" +
                "    public void onCreate(Bundle savedInstanceState) {\n" +
                "        Runnable lambda = () -> Log.d(\"MainActivity\", \"Hello, Lambda!\");\n" +
                "        lambda.run();\n" +
                "    }\n" +
                "}");

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("assembleDebug")
                .build();

        File mainClassFile = new File(buildFolder, "intermediates/transforms/CLASSES/PROJECT/retrolambda/debug/me/tatarka/retrolambda/MainActivity.class");
        File lambdaClassFile = new File(buildFolder, "intermediates/transforms/CLASSES/PROJECT/retrolambda/debug/me/tatarka/retrolambda/MainActivity$$Lambda$1.class");

        assertThat(mainClassFile).exists();
        assertThat(lambdaClassFile).exists();
    }
}
