package xyz.cloudkeeper.model.beans.element.module;

import org.testng.annotations.Factory;
import xyz.cloudkeeper.model.beans.MutableLocatableContract;

public class MutableModuleDeclarationTest {
    @Factory
    public Object[] contracts() {
        return MutableLocatableContract.contractsFor(MutableSimpleModuleDeclaration.class,
            MutableCompositeModuleDeclaration.class);
    }
}
