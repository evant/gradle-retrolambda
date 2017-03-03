package me.tatarka.retrolambda.sample.app;

import dagger.Component;

@Component(modules = MyModule.class)
public interface MyComponent {
    void inject(MainActivity activity);
}
