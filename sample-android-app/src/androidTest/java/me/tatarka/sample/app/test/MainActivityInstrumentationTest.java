package me.tatarka.sample.app.test;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import me.tatarka.retrolambda.sample.app.Function;
import me.tatarka.retrolambda.sample.app.MainActivity;
import me.tatarka.retrolambda.sample.app.R;

import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by evan on 3/29/15.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityInstrumentationTest extends ActivityInstrumentationTestCase2<MainActivity> {
    public MainActivityInstrumentationTest() {
        super(MainActivity.class);
    }
    
    @Before
    public void setup() {
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
    }
    
    @Test
    public void testHelloRetrolambda() {
        TextView text = (TextView) getActivity().findViewById(R.id.text);
        assertThat(text).hasText("Hello, retrolambda!");
    }
    
    @Test
    public void testHelloRetrolambdaLib() {
        TextView textLib = (TextView) getActivity().findViewById(R.id.text_lib);
        assertThat(textLib).hasText("Hello, retrolambda (from lib)!");
    }
    
    @Test
    public void testLambdaInTest() {
        Function lambda = () -> "test";
        assertThat(lambda.run()).isEqualTo("test");
    }
}
