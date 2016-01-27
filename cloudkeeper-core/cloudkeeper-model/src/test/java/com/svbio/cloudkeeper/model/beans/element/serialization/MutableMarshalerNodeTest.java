package com.svbio.cloudkeeper.model.beans.element.serialization;

import com.svbio.cloudkeeper.model.beans.MutableLocatableContract;
import org.testng.annotations.Factory;

public class MutableMarshalerNodeTest {
    @Factory
    public Object[] contracts() {
        return MutableLocatableContract.contractsFor(
            MutableSerializationRoot.class, MutableSerializedString.class, MutableByteSequence.class
        );
    }
}
