package xyz.cloudkeeper.maven;

import xyz.cloudkeeper.dsl.CompositeModule;
import xyz.cloudkeeper.dsl.CompositeModulePlugin;
import xyz.cloudkeeper.examples.modules.BinarySum;
import xyz.cloudkeeper.examples.modules.Decrease;

@CompositeModulePlugin("Computes the sum of two numbers minus two.")
public abstract class SumMinusTwo extends CompositeModule<SumMinusTwo> {
    public abstract InPort<Integer> num1();
    public abstract InPort<Integer> num2();
    public abstract OutPort<Integer> result();

    BinarySum sum = child(BinarySum.class)
        .num1().from(num1())
        .num2().from(num2());
    Decrease decrease = child(Decrease.class)
        .num().from(sum.sum());
    BinarySum secondDecrease = child(BinarySum.class)
        .num1().from(decrease.result())
        .num2().from(value(-1));

    { result().from(secondDecrease.sum()); }
}
