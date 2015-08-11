package me.tatarka.retrolambda.sample.app;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import javax.inject.Inject;

import me.tatarka.retrolambda.sample.app.databinding.ActivityMainBinding;
import me.tatarka.retrolambda.sample.lib.Function;

/**
 * Created by evan on 3/29/15.
 */
public class MainActivity extends AppCompatActivity {
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

        ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        TextView text = (TextView) findViewById(R.id.text);
        text.setText(hello.run(getResources()));

        TextView textLib = (TextView) findViewById(R.id.text_lib);
        textLib.setText(libHello.run());

        Function helloDatabinding = () -> "Hello, Retrolambda! (from databinding)";
        binding.setFun(helloDatabinding);
        binding.executePendingBindings();

        ResFunction lambda = (res) -> "Foo";
    }
}
