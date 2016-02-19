package xyz.cloudkeeper.model.beans.element;

import org.testng.annotations.Factory;
import xyz.cloudkeeper.model.beans.MutableLocatableContract;

public class MutablePackageTest {
    @Factory
    public Object[] contracts() {
        return MutableLocatableContract.contractsFor(MutablePackage.class);
    }
}
