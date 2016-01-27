package com.svbio.cloudkeeper.model.beans.element.module;

import com.svbio.cloudkeeper.model.beans.MutableLocatableContract;
import org.testng.annotations.Factory;

public class MutableModuleDeclarationTest {
    @Factory
    public Object[] contracts() {
        return MutableLocatableContract.contractsFor(MutableSimpleModuleDeclaration.class,
            MutableCompositeModuleDeclaration.class);
    }
}
