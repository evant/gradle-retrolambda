package me.tatarka.retrolambda.sample.app;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import javax.inject.Inject;

public class MainActivity extends AppCompatActivity {
    @Inject
    ResFunction hello;

    @Inject
    me.tatarka.retrolambda.sample.lib.Function libHello;

    @Inject
    me.tatarka.retrolambda.sample.feature.Function libFeature;

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

        TextView textFeature = (TextView) findViewById(R.id.text_feature);
        textFeature.setText(libFeature.run());

        ResFunction lambda = (res) -> "Foo2";

        Toast.makeText(this, lambda.run(null), Toast.LENGTH_SHORT).show();
    }
}
