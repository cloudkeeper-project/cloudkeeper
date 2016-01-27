package com.svbio.cloudkeeper.model.beans.element.annotation;

import com.svbio.cloudkeeper.model.beans.MutableLocatableContract;
import org.testng.annotations.Factory;

public class MutableAnnotationEntryTest {
    @Factory
    public Object[] contracts() {
        return MutableLocatableContract.contractsFor(MutableAnnotationEntry.class);
    }
}
