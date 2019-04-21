package me.tatarka;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Properties;

public class TestHelpers {

    /**
     * Taken from https://docs.gradle.org/current/userguide/test_kit.html and modified for java.
     */
    public static String getPluginClasspath() throws IOException {
        InputStream pluginClasspathResource = TestHelpers.class.getClassLoader().getResourceAsStream("plugin-classpath.txt");
        StringBuilder classpath = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(pluginClasspathResource));
        try {
            for (String line; (line = reader.readLine()) != null; ) {
                if (classpath.length() != 0) classpath.append(", ");
                line = line.replace("\\", "\\\\"); // escape backslashes in Windows paths
                classpath.append("'").append(line).append("'");
            }
        } finally {
            reader.close();
        }
        return classpath.toString();
    }

    public static void writeFile(File file, String content) throws IOException {
        //noinspection ResultOfMethodCallIgnored
        file.getParentFile().mkdirs();
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        try {
            writer.write(content);
        } finally {
            writer.close();
        }
    }

    public static String[] oldestSupportedGradleVersion() {
        return new String[]{"2.6"};
    }

    public static String[] newestSupportedGradleVersion() {
        return new String[]{"5.1.1"};
    }

    public static String[] oldestSupportedAndroidPluginVersion() {
        return new String[]{
                /*androidPluginVersion=*/"1.5.0",
                /*gradleVersion=*/"2.6",
                /*buildToolsVersion=*/"24.0.3",
                /*kotlinVersion=*/"1.1.0",
        };
    }

    public static String[] oldestSupportedAndroidFeaturePluginVersion() {
        return new String[]{
                /*androidPluginVersion=*/"3.1.0",
                /*gradleVersion=*/"4.4",
                /*buildToolsVersion=*/"26.0.2",
                /*kotlinVersion=*/"1.1.0",
        };
    }

    public static String[] newestSupportedAndroidPluginVersion() {
        return new String[]{
                /*androidPluginVersion=*/currentAndroidPluginVersion(),
                /*gradleVersion=*/"5.4",
                /*buildToolsVersion=*/"28.0.3",
                /*kotlinVersion=*/"1.3.10",
        };
    }

    private static String currentAndroidPluginVersion() {
        try {
            Properties properties = new Properties();
            properties.load(new FileInputStream("../gradle.properties"));
            return properties.getProperty("androidPluginVersion");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static File findFile(File dir, final String name) throws IOException {
        final File[] result = new File[1];
        Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                if (path.endsWith(name)) {
                    result[0] = path.toFile();
                    return FileVisitResult.TERMINATE;
                } else {
                    return FileVisitResult.CONTINUE;
                }
            }
        });
        return result[0];
    }

    public static void copyLocalPropertiesIfExists(File destinationDir) throws IOException {
        File localProperties = new File("../local.properties");
        if (localProperties.exists()) {
            FileUtils.copyFile(localProperties, new File(destinationDir, "local.properties"));
        }
    }
}
