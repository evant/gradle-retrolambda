package me.tatarka.retrolambda.sample.app.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import me.tatarka.retrolambda.sample.app.Function;
import me.tatarka.retrolambda.sample.app.MainActivity;
import me.tatarka.retrolambda.sample.app.MyModule;
import me.tatarka.retrolambda.sample.lib.Lib;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by evan on 3/29/15.
 */
@RunWith(JUnit4.class)
public class FunctionTest {
    MyModule module = new MyModule();
    
    @Test
    public void testGetHello() {
        assertThat(module.provideHello().run()).isEqualTo("Hello, retrolambda!");
    }

    @Test
    public void testGetHelloLib() {
        assertThat(module.provideLibHello().run()).isEqualTo("Hello, retrolambda (from lib)!");
    }
    
    @Test
    public void testLambdaInTest() {
        Function lambda = () -> "test";
        assertThat(lambda.run()).isEqualTo("test");
    }
}
