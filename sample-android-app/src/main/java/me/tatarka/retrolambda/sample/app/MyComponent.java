package me.tatarka.retrolambda.sample.app;

import dagger.Component;

/**
 * Created by evan on 4/2/15.
 */
@Component(modules = MyModule.class)
public interface MyComponent {
    void inject(MainActivity activity);
}
