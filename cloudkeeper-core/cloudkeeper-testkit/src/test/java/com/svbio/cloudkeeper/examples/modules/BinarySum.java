package com.svbio.cloudkeeper.examples.modules;

import com.svbio.cloudkeeper.dsl.FromConnectable;
import com.svbio.cloudkeeper.dsl.SimpleModule;
import com.svbio.cloudkeeper.dsl.SimpleModulePlugin;

@SimpleModulePlugin("Computes the sum of two numbers.")
public abstract class BinarySum extends SimpleModule<BinarySum> {
    public abstract InPort<Integer> num1();
    public abstract InPort<Integer> num2();
    public abstract OutPort<Integer> sum();

    public BinarySum from(FromConnectable<Integer> num1, FromConnectable<Integer> num2) {
        return num1().from(num1). num2().from(num2);
    }

    @Override
    public void run() {
        sum().set(num1().get() + num2().get());
    }
}
