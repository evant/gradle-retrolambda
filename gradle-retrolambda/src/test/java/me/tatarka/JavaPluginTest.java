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
import static org.assertj.core.api.Assertions.fail;

@RunWith(JUnit4.class)
public class JavaPluginTest {
    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder();
    private File rootDir;
    private File buildFile;

    @Before
    public void setup() throws Exception {
        rootDir = testProjectDir.getRoot();
        buildFile = testProjectDir.newFile("build.gradle");
    }

    @Test
    public void assemble() throws Exception {
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

        File javaFile = new File(rootDir, "src/main/java/Main.java");

        writeFile(javaFile, "public class Main {\n" +
                "    public static void main(String[] args) {\n" +
                "        Runnable lambda = () -> System.out.println(\"Hello, Lambda!\");\n" +
                "        lambda.run();\n" +
                "    }\n" +
                "}");

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("assemble", "--stacktrace")
                .build();

        assertThat(result.getStandardError()).isNullOrEmpty();

        File mainClassFile = new File(rootDir, "build/classes/main/Main.class");
        File lambdaClassFile = new File(rootDir, "build/classes/main/Main$$Lambda$1.class");

        assertThat(mainClassFile).exists();
        assertThat(lambdaClassFile).exists();
    }

    @Test
    public void test() throws Exception {
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
                "}\n" +
                "\n" +
                "dependencies {\n" +
                "    testCompile 'junit:junit:4.12'\n" +
                "}\n" +
                "\n" +
                "test {\n" +
                "    testLogging { events \"failed\" }\n" +
                "}");

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

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("test", "--stacktrace")
                .build();

        assertThat(result.getStandardError()).isNullOrEmpty();
    }

    @Test
    public void run() throws Exception {
        writeBuildFile(buildFile, "buildscript {\n" +
                "    dependencies {\n" +
                "        classpath files($pluginClasspath)\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "apply plugin: 'java'\n" +
                "apply plugin: 'application'\n" +
                "apply plugin: 'me.tatarka.retrolambda'\n" +
                "\n" +
                "repositories {\n" +
                "    mavenCentral()\n" +
                "}\n" +
                "\n" +
                "mainClassName = \"Main\"\n" +
                "\n" +
                "jar {\n" +
                "    manifest {\n" +
                "        attributes 'Main-Class': mainClassName\n" +
                "    }\n" +
                "}");

        File javaFile = new File(rootDir, "src/main/java/Main.java");

        writeFile(javaFile, "public class Main {\n" +
                "    public static void main(String[] args) {\n" +
                "        Runnable lambda = () -> System.out.println(\"Hello, Lambda Run!\");\n" +
                "        lambda.run();\n" +
                "    }\n" +
                "}");

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("run")
                .build();

        assertThat(result.getStandardError()).isNullOrEmpty();
        assertThat(result.getStandardOutput()).contains("Hello, Lambda Run!");
    }
}
