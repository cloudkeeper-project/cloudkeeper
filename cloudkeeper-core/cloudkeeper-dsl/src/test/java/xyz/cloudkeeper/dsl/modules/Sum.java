package xyz.cloudkeeper.dsl.modules;

import xyz.cloudkeeper.dsl.CompositeModule;

public abstract class Sum extends CompositeModule<Sum> {
    public abstract InPort<Integer> firstPort();
    public abstract InPort<Integer> secondPort();
    public abstract OutPort<Integer> outPort();
}
