package com.svbio.cloudkeeper.model.beans.element;

import com.svbio.cloudkeeper.model.beans.MutableLocatableContract;
import org.testng.annotations.Factory;

public class MutableNameableTest {
    @Factory
    public Object[] contracts() {
        return MutableLocatableContract.contractsFor(MutableQualifiedNamable.class, MutableSimpleNameable.class);
    }
}
