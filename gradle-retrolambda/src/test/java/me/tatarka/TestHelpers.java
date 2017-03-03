package me.tatarka;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    public static String[] oldestSupportedAndroidPluginVersion() {
        return new String[]{
                /*androidPluginVersion=*/"1.5.0",
                /*gradleVersion=*/"2.5",
                /*buildToolsVersion=*/"24.0.2"
        };
    }

    public static String[] newestSupportedAndroidPluginVersion() {
        return new String[]{
                /*androidPluginVersion=*/currentAndroidPluginVersion(),
                /*gradleVersion=*/"3.4",
                /*buildToolsVersion=*/"25.0.0"
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
}
