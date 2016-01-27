package com.svbio.cloudkeeper.model.beans.type;

import com.svbio.cloudkeeper.model.beans.MutableLocatableContract;
import org.testng.annotations.Factory;

public class MutableTypeMirrorTest {
    @Factory
    public Object[] contracts() {
        return MutableLocatableContract.contractsFor(MutableArrayType.class, MutableDeclaredType.class,
            MutablePrimitiveType.class, MutableTypeVariable.class, MutableWildcardType.class);
    }
}
