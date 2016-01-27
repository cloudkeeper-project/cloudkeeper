package com.svbio.cloudkeeper.model.beans.element;

import com.svbio.cloudkeeper.model.beans.MutableLocatableContract;
import org.testng.annotations.Factory;

public class MutablePackageTest {
    @Factory
    public Object[] contracts() {
        return MutableLocatableContract.contractsFor(MutablePackage.class);
    }
}
