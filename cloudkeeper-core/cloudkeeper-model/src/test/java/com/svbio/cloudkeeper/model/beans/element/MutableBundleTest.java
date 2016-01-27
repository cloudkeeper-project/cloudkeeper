package com.svbio.cloudkeeper.model.beans.element;

import com.svbio.cloudkeeper.model.beans.MutableLocatableContract;
import org.testng.annotations.Factory;

public class MutableBundleTest {
    @Factory
    public Object[] contracts() {
        return MutableLocatableContract.contractsFor(MutableBundle.class);
    }
}
