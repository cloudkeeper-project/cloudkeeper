package com.svbio.cloudkeeper.model.beans.element.annotation;

import com.svbio.cloudkeeper.model.beans.MutableLocatableContract;
import org.testng.annotations.Factory;

public class MutableAnnotationTypeElementTest {
    @Factory
    public Object[] contracts() {
        return MutableLocatableContract.contractsFor(MutableAnnotationTypeElement.class);
    }
}
