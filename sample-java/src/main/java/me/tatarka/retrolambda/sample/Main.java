package me.tatarka.retrolambda.sample;

/**
 * Created by evan on 3/29/15.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println(getHello().run());
    }
    
    public static Function getHello() {
        return () -> "Hello, retrolambda!";
    } 
}
