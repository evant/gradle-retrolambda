package me.tatarka.retrolambda.sample.app;

import android.content.res.Resources;

import dagger.Module;
import dagger.Provides;
import me.tatarka.retrolambda.sample.feature.Feature;
import me.tatarka.retrolambda.sample.lib.Lib;

@Module
public class MyModule {
    @Provides
    public ResFunction provideHello() {
        return this::getHello;
    }

    private String getHello(Resources resources) {
        ResFunction f = (res) -> {
            return res.getString(R.string.hello);
        };
        return f.run(resources);
    }

    @Provides
    public me.tatarka.retrolambda.sample.lib.Function provideLibHello() {
        return Lib.getHello(Lib.MODE_A);
    }

    @Provides
    public me.tatarka.retrolambda.sample.feature.Function provideFeatureHello() {
        return Feature.getHello(Lib.MODE_A);
    }
}
