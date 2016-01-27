package com.svbio.cloudkeeper.model.beans.element.module;

import com.svbio.cloudkeeper.model.beans.MutableLocatableContract;
import org.testng.annotations.Factory;

public class MutablePortTest {
    @Factory
    public Object[] contracts() {
        return MutableLocatableContract.contractsFor(MutableInPort.class, MutableOutPort.class, MutableIOPort.class);
    }
}
