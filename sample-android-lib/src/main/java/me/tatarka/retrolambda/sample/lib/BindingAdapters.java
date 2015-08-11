package me.tatarka.retrolambda.sample.lib;

import android.databinding.BindingAdapter;
import android.widget.TextView;

public class BindingAdapters {
    @BindingAdapter("android:text")
    public static void setTextFunction(TextView textView, Function function) {
        String value = function.run();
        textView.setText(value);
    }
}