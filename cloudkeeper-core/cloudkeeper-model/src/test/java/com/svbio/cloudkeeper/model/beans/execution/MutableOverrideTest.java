package com.svbio.cloudkeeper.model.beans.execution;

import com.svbio.cloudkeeper.model.beans.MutableLocatableContract;
import org.testng.annotations.Factory;

public class MutableOverrideTest {
    @Factory
    public Object[] contracts() {
        return MutableLocatableContract.contractsFor(MutableOverride.class);
    }
}
