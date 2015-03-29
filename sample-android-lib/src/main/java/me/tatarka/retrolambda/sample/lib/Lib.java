package me.tatarka.retrolambda.sample.lib;

/**
 * Created by evan on 3/29/15.
 */
public class Lib {
    public static Function getHello() {
        return () -> "Hello, retrolambda (from lib)!";
    }
}
