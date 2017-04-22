package me.tatarka.retrolamba.sample.test;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.widget.TextView;
import me.tatarka.retrolambda.sample.app.MainActivity;
import me.tatarka.retrolambda.sample.app.R;
import me.tatarka.retrolambda.sample.app.ResFunction;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.assertj.core.api.Java6Assertions.assertThat;

@RunWith(AndroidJUnit4.class)
public class MainActivityInstrumentationTest {

    @Rule
    public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(MainActivity.class);

    @Test
    public void testHelloRetrolambda() {
        TextView text = (TextView) activityRule.getActivity().findViewById(R.id.text);
        assertThat(text.getText().toString()).isEqualTo("Hello, Retrolambda!");
    }

    @Test
    public void testHelloRetrolambdaLib() {
        TextView textLib = (TextView) activityRule.getActivity().findViewById(R.id.text_lib);
        assertThat(textLib.getText().toString()).isEqualTo("Hello, Retrolambda (from lib, mode a)!");
    }

    @Test
    public void testLambdaInTest() {
        ResFunction lambda = (res) -> "test";
        assertThat(lambda.run(activityRule.getActivity().getResources())).isEqualTo("test");
    }

    @Test
    public void testWithEspresso() {
        onView(withId(R.id.text)).check((view, ex) -> matches(withText("Hello, Retrolambda!")));
        onView(withId(R.id.text_lib)).check(matches(withText("Hello, Retrolambda (from lib, mode a)!")));
    }
}
