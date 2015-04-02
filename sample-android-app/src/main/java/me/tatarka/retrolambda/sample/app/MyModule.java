package me.tatarka.retrolambda.sample.app;

import dagger.Module;
import dagger.Provides;
import me.tatarka.retrolambda.sample.lib.Lib;

/**
 * Created by evan on 4/2/15.
 */
@Module
public class MyModule {
    @Provides
    public Function provideHello() {
        return () -> "Hello, retrolambda!";
    }

    @Provides
    public me.tatarka.retrolambda.sample.lib.Function provideLibHello() {
        return Lib.getHello();
    }
}
