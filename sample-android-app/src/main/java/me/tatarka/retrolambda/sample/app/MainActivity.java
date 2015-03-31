package me.tatarka.retrolambda.sample.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import me.tatarka.retrolambda.sample.lib.Lib;

/**
 * Created by evan on 3/29/15.
 */
public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        TextView text = (TextView) findViewById(R.id.text);
        text.setText(getHello().run());
        
        TextView textLib = (TextView) findViewById(R.id.text_lib);
        textLib.setText(Lib.getHello().run());
    }
    
    public static Function getHello() {
        return () -> "Hello, retrolambda!";
    }
}
