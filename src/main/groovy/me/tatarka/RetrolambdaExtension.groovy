package me.tatarka
/**
 * Created with IntelliJ IDEA.
 * User: evan
 * Date: 8/4/13
 * Time: 1:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class RetrolambdaExtension {
    Object compile = "net.orfjackal.retrolambda:retrolambda:1.1.0"
    String jdk = System.getenv("JAVA_HOME")
    int bytecodeVersion = 50
    List<String> excludes = []
    List<String> includes = []

    public void exclude(Object... e) {
        excludes.addAll(e.collect {i -> i.toString()})
    }

    public void include(Object... e) {
        includes.addAll(e.collect {i -> i.toString()})
    }

    public void setCompile(Object c) {
        compile = c
    }

    public void setBytecodeVersion(int v) {
        bytecodeVersion = v
    }

    public void setJavaVersion(Object v) {
        switch (v.toString()) {
            case '1.6':
            case '6': bytecodeVersion = 50
                break
            case '1.8':
            case '7': bytecodeVersion = 51
                break
            default:
                throw new RuntimeException("Unknown java version: $v, only 6 or 7 are accepted")
        }
    }

    public void setJdk(String path) {
        jdk = path
    }

    public boolean isIncluded(String name) {
        if (includes.isEmpty() && excludes.isEmpty()) return true
        if (excludes.isEmpty() && excludes.contains(name)) return false;
        if (includes.isEmpty() && !includes.contains(name)) return false;
        return true
    }
}
