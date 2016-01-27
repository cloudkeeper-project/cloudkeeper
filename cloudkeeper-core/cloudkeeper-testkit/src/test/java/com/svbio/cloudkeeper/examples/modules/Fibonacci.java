package com.svbio.cloudkeeper.examples.modules;

import com.svbio.cloudkeeper.dsl.CompositeModule;
import com.svbio.cloudkeeper.dsl.CompositeModulePlugin;
import com.svbio.cloudkeeper.dsl.LoopModule;

@CompositeModulePlugin("Computes the n-th Fibonacci number for n >= 2.")
public abstract class Fibonacci extends CompositeModule<Fibonacci> {
    public abstract InPort<Integer> n();
    public abstract OutPort<Integer> result();

    public abstract static class Loop extends LoopModule<Loop> {
        public abstract IOPort<Integer> count();
        public abstract IOPort<Integer> last();
        public abstract IOPort<Integer> secondLast();

        BinarySum sum = child(BinarySum.class).from(last(), secondLast());
        Decrease decr = child(Decrease.class).from(count());
        GreaterOrEqual gt = child(GreaterOrEqual.class).from(decr.result(), value(2));

        // Link outputs
        { count().from(decr.result()); }
        { last().from(sum.sum()); }
        { secondLast().from(last()); }

        // Looping condition
        { repeatIf(gt.result()); }
    }
    Loop loop = child(Loop.class)
        .count().from(n())
        .last().from(value(1))
        .secondLast().from(value(0));

    // Link outputs
    { result().from(loop.last()); }
}
