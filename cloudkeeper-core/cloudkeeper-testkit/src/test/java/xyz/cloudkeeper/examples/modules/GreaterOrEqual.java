package xyz.cloudkeeper.examples.modules;

import xyz.cloudkeeper.dsl.FromConnectable;
import xyz.cloudkeeper.dsl.SimpleModule;
import xyz.cloudkeeper.dsl.SimpleModulePlugin;

@SimpleModulePlugin("Compare two numbers.")
public abstract class GreaterOrEqual extends SimpleModule<GreaterOrEqual> {
    public abstract InPort<Integer> num1();
    public abstract InPort<Integer> num2();
    public abstract OutPort<Boolean> result();

    public GreaterOrEqual from(FromConnectable<Integer> num1, FromConnectable<Integer> num2) {
        return num1().from(num1)
            .num2().from(num2);
    }

    @Override
    public void run() {
        result().set(num1().get() >= num2().get());
    }
}
