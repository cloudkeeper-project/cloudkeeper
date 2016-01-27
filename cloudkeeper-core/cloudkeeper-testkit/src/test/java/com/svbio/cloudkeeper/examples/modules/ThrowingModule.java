package com.svbio.cloudkeeper.examples.modules;

import com.svbio.cloudkeeper.dsl.SimpleModule;
import com.svbio.cloudkeeper.dsl.SimpleModulePlugin;

@SimpleModulePlugin("Always throws a custom exception.")
public abstract class ThrowingModule extends SimpleModule<ThrowingModule> {
    public abstract InPort<String> string();
    public abstract OutPort<Long> size();

    @Override
    public void run() throws ExpectedException {
        throw new ExpectedException();
    }

    public static class ExpectedException extends Exception {
        private static final long serialVersionUID = -3363611000366483460L;

        ExpectedException() {
            super("This is an expected exception.");
        }
    }
}
