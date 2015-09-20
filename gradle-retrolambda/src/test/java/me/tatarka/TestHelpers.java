package me.tatarka;

import java.io.*;

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

    public static void writeBuildFile(File buildFile, String content) throws IOException {
        writeFile(buildFile, content.replace("$pluginClasspath", getPluginClasspath()));
    }

    public static void writeFile(File file, String content) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        try {
            writer.write(content);
        } finally {
            writer.close();
        }
    }
}
