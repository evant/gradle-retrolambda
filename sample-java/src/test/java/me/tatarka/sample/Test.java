package me.tatarka.sample;

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by evan on 3/29/15.
 */
@RunWith(JUnit4.class)
public class Test {
    @org.junit.Test
    public void testGetHello() {
        assertThat(Main.getHello().run()).isEqualTo("Hello, retrolambda!");
    }
}
