package me.tatarka.retrolambda.sample.app.test;

import android.content.res.Resources;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import me.tatarka.retrolambda.sample.app.R;
import me.tatarka.retrolambda.sample.app.ResFunction;
import me.tatarka.retrolambda.sample.app.MyModule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by evan on 3/29/15.
 */
@RunWith(JUnit4.class)
public class FunctionTest {
    MyModule module = new MyModule();

    @Test
    public void testGetHello() {
        Resources res = mock(Resources.class);
        when(res.getString(R.string.hello)).thenReturn("Hello, Retrolambda!");
        assertThat(module.provideHello().run(res)).isEqualTo("Hello, Retrolambda!");
    }

    @Test
    public void testGetHelloLib() {
        assertThat(module.provideLibHello().run()).isEqualTo("Hello, Retrolambda (from lib, mode a)!");
    }

    @Test
    public void testLambdaInTest() {
        ResFunction lambda = (res) -> "test";
        Resources res = mock(Resources.class);
        assertThat(lambda.run(res)).isEqualTo("test");
    }
}
