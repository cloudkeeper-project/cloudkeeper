package xyz.cloudkeeper.model.beans.element.annotation;

import org.testng.annotations.Factory;
import xyz.cloudkeeper.model.beans.MutableLocatableContract;

public class MutableAnnotationTypeElementTest {
    @Factory
    public Object[] contracts() {
        return MutableLocatableContract.contractsFor(MutableAnnotationTypeElement.class);
    }
}
