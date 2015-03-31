package me.tatarka.retrolambda.sample;

/**
 * Created by evan on 3/29/15.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println(Interface.staticMethod());
        Impl impl = new Impl();
        System.out.println(impl.defaultMethod());
    }
    
    private static class Impl implements Interface {
    }
}
