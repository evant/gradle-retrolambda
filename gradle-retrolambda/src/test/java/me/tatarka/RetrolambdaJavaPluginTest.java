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
public class RetrolambdaJavaPluginTest {
    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder();
    private File buildFile;
    private File javaFolder;
    private File buildFolder;

    @Before
    public void setup() throws Exception {
        buildFile = testProjectDir.newFile("build.gradle");
        javaFolder = new File(testProjectDir.getRoot(), "src/main/java");
        if (!javaFolder.mkdirs()) {
            throw new IOException();
        }
        buildFolder = new File(testProjectDir.getRoot(), "build");
    }

    @Test
    public void testAssemble() throws Exception {
        writeBuildFile(buildFile, "buildscript {\n" +
                "    dependencies {\n" +
                "        classpath files($pluginClasspath)\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "apply plugin: 'java'\n" +
                "apply plugin: 'me.tatarka.retrolambda'\n" +
                "\n" +
                "repositories {\n" +
                "    mavenCentral()\n" +
                "}");

        File javaFile = new File(javaFolder, "Main.java");

        writeFile(javaFile, "public class Main {\n" +
                "    public static void main(String[] args) {\n" +
                "        Runnable lambda = () -> System.out.println(\"Hello, Lambda!\");\n" +
                "        lambda.run();\n" +
                "    }\n" +
                "}");

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("assemble")
                .build();

        File mainClassFile = new File(buildFolder, "classes/main/Main.class");
        File lambdaClassFile = new File(buildFolder, "classes/main/Main$$Lambda$1.class");

        assertThat(mainClassFile).exists();
        assertThat(lambdaClassFile).exists();
    }
}
