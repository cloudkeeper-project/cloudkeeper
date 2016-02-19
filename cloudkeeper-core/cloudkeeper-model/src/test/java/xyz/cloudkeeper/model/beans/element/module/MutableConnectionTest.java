package xyz.cloudkeeper.model.beans.element.module;

import org.testng.annotations.Factory;
import xyz.cloudkeeper.model.beans.MutableLocatableContract;

public class MutableConnectionTest {
    @Factory
    public Object[] contracts() {
        return MutableLocatableContract.contractsFor(MutableParentInToChildInConnection.class,
            MutableSiblingConnection.class, MutableChildOutToParentOutConnection.class,
            MutableShortCircuitConnection.class);
    }
}
