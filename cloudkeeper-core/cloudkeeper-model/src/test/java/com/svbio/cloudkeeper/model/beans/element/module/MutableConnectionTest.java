package com.svbio.cloudkeeper.model.beans.element.module;

import com.svbio.cloudkeeper.model.beans.MutableLocatableContract;
import org.testng.annotations.Factory;

public class MutableConnectionTest {
    @Factory
    public Object[] contracts() {
        return MutableLocatableContract.contractsFor(MutableParentInToChildInConnection.class,
            MutableSiblingConnection.class, MutableChildOutToParentOutConnection.class,
            MutableShortCircuitConnection.class);
    }
}
