package xyz.cloudkeeper.model.beans.type;

import org.testng.annotations.Factory;
import xyz.cloudkeeper.model.beans.MutableLocatableContract;

public class MutableTypeMirrorTest {
    @Factory
    public Object[] contracts() {
        return MutableLocatableContract.contractsFor(MutableArrayType.class, MutableDeclaredType.class,
            MutablePrimitiveType.class, MutableTypeVariable.class, MutableWildcardType.class);
    }
}
