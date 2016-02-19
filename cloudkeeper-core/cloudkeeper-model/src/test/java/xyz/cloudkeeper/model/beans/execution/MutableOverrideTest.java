package xyz.cloudkeeper.model.beans.execution;

import org.testng.annotations.Factory;
import xyz.cloudkeeper.model.beans.MutableLocatableContract;

public class MutableOverrideTest {
    @Factory
    public Object[] contracts() {
        return MutableLocatableContract.contractsFor(MutableOverride.class);
    }
}
