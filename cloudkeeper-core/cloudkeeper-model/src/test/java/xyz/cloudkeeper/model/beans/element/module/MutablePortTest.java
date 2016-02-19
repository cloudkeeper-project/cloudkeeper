package xyz.cloudkeeper.model.beans.element.module;

import org.testng.annotations.Factory;
import xyz.cloudkeeper.model.beans.MutableLocatableContract;

public class MutablePortTest {
    @Factory
    public Object[] contracts() {
        return MutableLocatableContract.contractsFor(MutableInPort.class, MutableOutPort.class, MutableIOPort.class);
    }
}
