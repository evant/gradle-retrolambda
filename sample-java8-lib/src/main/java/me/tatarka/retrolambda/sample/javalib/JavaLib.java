package me.tatarka.retrolambda.sample.javalib;

public class JavaLib {
    public static Function getHello() {
        return new Function() {
            @Override
            public String run() {
                return "Hello, Retrolambda (from java lib)!";
            }
        };
    }
}
