package me.tatarka

/**
 * Created by evan on 8/5/13.
 */
class JdkPathException extends RuntimeException {
    public JdkPathException(String path) {
        super("$path does not exist, make sure that JAVE_HOME or retrolambda.jdk points to a valid version of java8\n You can download java8 from https://jdk8.java.net/lambda")
    }
}
