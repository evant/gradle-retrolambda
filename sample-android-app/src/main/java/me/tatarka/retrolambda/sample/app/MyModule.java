package me.tatarka.retrolambda.sample.app;

import android.content.res.Resources;
import dagger.Module;
import dagger.Provides;
import me.tatarka.retrolambda.sample.lib.Lib;

/**
 * Created by evan on 4/2/15.
 */
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
        return Lib.getHello();
    }
}
