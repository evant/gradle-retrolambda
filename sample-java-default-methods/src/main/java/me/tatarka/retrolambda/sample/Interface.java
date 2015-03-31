package me.tatarka.retrolambda.sample;

/**
 * Created by evan on 3/29/15.
 */
public interface Interface {
    static String staticMethod() {
        return "Hello, retrolambda (from static method in interface)!";
    }
    
    default String defaultMethod() {
        return "Hello, retrolambda (from default method in interface)!";
    }
}
