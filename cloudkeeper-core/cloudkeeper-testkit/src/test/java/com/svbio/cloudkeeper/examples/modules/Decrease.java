package com.svbio.cloudkeeper.examples.modules;

import com.svbio.cloudkeeper.dsl.FromConnectable;
import com.svbio.cloudkeeper.dsl.SimpleModule;
import com.svbio.cloudkeeper.dsl.SimpleModulePlugin;

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
