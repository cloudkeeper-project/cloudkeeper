package xyz.cloudkeeper.examples.modules;

import xyz.cloudkeeper.dsl.SimpleModule;
import xyz.cloudkeeper.dsl.SimpleModulePlugin;

@SimpleModulePlugin("Module that waits for a given amount of seconds and returns a hello-world string.")
public abstract class DelayingModule extends SimpleModule<DelayingModule> {
    public abstract InPort<Long> delaySeconds();
    public abstract OutPort<String> helloWorld();

    @Override
    public void run() throws InterruptedException {
        Thread.sleep(delaySeconds().get() * 1000);
        helloWorld().set("Hello World!");
    }
}
