package me.tatarka.retrolambda.sample.test;

import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import me.tatarka.retrolambda.sample.Main;

/**
 * Created by evan on 3/29/15.
 */
@RunWith(JUnit4.class)
public class Test {
    @org.junit.Test
    public void testGetHello() {
        Assert.assertEquals("Hello, retrolambda!", Main.getHello().run());
    }
}
