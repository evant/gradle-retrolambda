package me.tatarka.retrolambda.sample.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import javax.inject.Inject;

/**
 * Created by evan on 3/29/15.
 */
public class MainActivity extends Activity {
    @Inject
    ResFunction hello;

    @Inject
    me.tatarka.retrolambda.sample.lib.Function libHello;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DaggerMyComponent.builder()
                .myModule(new MyModule())
                .build()
                .inject(this);

        setContentView(R.layout.activity_main);

        TextView text = (TextView) findViewById(R.id.text);
        text.setText(hello.run(getResources()));

        TextView textLib = (TextView) findViewById(R.id.text_lib);
        textLib.setText(libHello.run());
    }
}
