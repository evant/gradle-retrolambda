package me.tatarka.retrolambda.sample.lib;

import android.support.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by evan on 3/29/15.
 */
public class Lib {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({MODE_A, MODE_B})
    public @interface Mode {}

    public static final int MODE_A = 1;
    public static final int MODE_B = 2;


    public static Function getHello(@Mode int mode) {
        if (mode == MODE_A) {
            return () -> "Hello, Retrolambda (from lib, mode a)!";
        } else {
            return () -> "Hello, Retrolambda (from lib, mode b)!";
        }
    }
}
