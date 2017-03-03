package me.tatarka;


import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.StringWriter;

import static me.tatarka.TestHelpers.getPluginClasspath;
import static me.tatarka.TestHelpers.writeFile;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class GroovyPluginTest {
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
        writeFile(buildFile, "buildscript {\n" +
                "    dependencies {\n" +
                "        classpath files(" + getPluginClasspath() + ")\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "apply plugin: 'groovy'\n" +
                "apply plugin: 'me.tatarka.retrolambda'\n" +
                "\n" +
                "repositories {\n" +
                "    mavenCentral()\n" +
                "}\n" +
                "\n" +
                "dependencies {\n" +
                "    compile 'org.codehaus.groovy:groovy-all:2.4.7'\n" +
                "}");

        File javaFile = new File(rootDir, "src/main/java/Main.java");

        writeFile(javaFile, "public class Main {\n" +
                "    public static void main(String[] args) {\n" +
                "        Runnable lambda = () -> System.out.println(\"Hello, Lambda!\");\n" +
                "        lambda.run();\n" +
                "    }\n" +
                "}");

        StringWriter errorOutput = new StringWriter();
        BuildResult result = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments("assemble", "--stacktrace")
                .forwardStdError(errorOutput)
                .build();

        assertThat(result.task(":assemble").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);

        File mainClassFile = new File(rootDir, "build/classes/main/Main.class");
        File lambdaClassFile = new File(rootDir, "build/classes/main/Main$$Lambda$1.class");

        assertThat(mainClassFile).exists();
        assertThat(lambdaClassFile).exists();
    }

    @Test
    public void test() throws Exception {
        writeFile(buildFile, "buildscript {\n" +
                "    dependencies {\n" +
                "        classpath files(" + getPluginClasspath() + ")\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "apply plugin: 'groovy'\n" +
                "apply plugin: 'me.tatarka.retrolambda'\n" +
                "\n" +
                "repositories {\n" +
                "    mavenCentral()\n" +
                "}\n" +
                "\n" +
                "dependencies {\n" +
                "    compile 'org.codehaus.groovy:groovy-all:2.4.7'\n" +
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

        StringWriter errorOutput = new StringWriter();
        BuildResult result = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments("test", "--stacktrace")
                .forwardStdError(errorOutput)
                .build();

        assertThat(result.task(":test").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    }

    @Test
    public void run() throws Exception {
        writeFile(buildFile, "buildscript {\n" +
                "    dependencies {\n" +
                "        classpath files(" + getPluginClasspath() + ")\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "apply plugin: 'groovy'\n" +
                "apply plugin: 'application'\n" +
                "apply plugin: 'me.tatarka.retrolambda'\n" +
                "\n" +
                "repositories {\n" +
                "    mavenCentral()\n" +
                "}\n" +
                "\n" +
                "dependencies {\n" +
                "    compile 'org.codehaus.groovy:groovy-all:2.4.7'\n" +
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

        StringWriter errorOutput = new StringWriter();
        BuildResult result = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments("run")
                .forwardStdError(errorOutput)
                .build();

        assertThat(result.task(":run").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(result.getOutput()).contains("Hello, Lambda Run!");
    }
}
