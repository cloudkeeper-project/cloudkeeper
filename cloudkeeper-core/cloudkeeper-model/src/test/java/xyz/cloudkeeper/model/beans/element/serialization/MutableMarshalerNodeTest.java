package xyz.cloudkeeper.model.beans.element.serialization;

import org.testng.annotations.Factory;
import xyz.cloudkeeper.model.beans.MutableLocatableContract;

public class MutableMarshalerNodeTest {
    @Factory
    public Object[] contracts() {
        return MutableLocatableContract.contractsFor(
            MutableSerializationRoot.class, MutableSerializedString.class, MutableByteSequence.class
        );
    }
}
