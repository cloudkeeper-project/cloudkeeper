package xyz.cloudkeeper.examples.modules;

import xyz.cloudkeeper.dsl.FromConnectable;
import xyz.cloudkeeper.dsl.SimpleModule;
import xyz.cloudkeeper.dsl.SimpleModulePlugin;

@SimpleModulePlugin("Decrease integer by one.")
public abstract class Decrease extends SimpleModule<Decrease> {
    public abstract InPort<Integer> num();
    public abstract OutPort<Integer> result();

    // Integer is final, so no need to write "? extends Integer".
    public Decrease from(FromConnectable<Integer> fromPort) {
        return num().from(fromPort);
    }

    @Override
    public void run() {
        result().set(num().get() - 1);
    }
}
