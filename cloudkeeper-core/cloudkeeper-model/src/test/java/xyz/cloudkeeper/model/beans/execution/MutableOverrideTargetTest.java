package xyz.cloudkeeper.model.beans.execution;

import org.testng.annotations.Factory;
import xyz.cloudkeeper.model.beans.MutableLocatableContract;

public class MutableOverrideTargetTest {
    @Factory
    public Object[] contracts() {
        return MutableLocatableContract.contractsFor(MutableElementPatternTarget.class, MutableElementTarget.class,
            MutableExecutionTracePatternTarget.class, MutableExecutionTraceTarget.class);
    }
}
