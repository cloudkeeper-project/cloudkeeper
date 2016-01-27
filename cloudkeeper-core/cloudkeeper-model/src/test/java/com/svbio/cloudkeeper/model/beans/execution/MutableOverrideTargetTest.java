package com.svbio.cloudkeeper.model.beans.execution;

import com.svbio.cloudkeeper.model.beans.MutableLocatableContract;
import org.testng.annotations.Factory;

public class MutableOverrideTargetTest {
    @Factory
    public Object[] contracts() {
        return MutableLocatableContract.contractsFor(MutableElementPatternTarget.class, MutableElementTarget.class,
            MutableExecutionTracePatternTarget.class, MutableExecutionTraceTarget.class);
    }
}
