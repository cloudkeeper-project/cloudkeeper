package com.svbio.cloudkeeper.dsl.modules;

import com.svbio.cloudkeeper.dsl.CompositeModule;

public abstract class Sum extends CompositeModule<Sum> {
    public abstract InPort<Integer> firstPort();
    public abstract InPort<Integer> secondPort();
    public abstract OutPort<Integer> outPort();
}
