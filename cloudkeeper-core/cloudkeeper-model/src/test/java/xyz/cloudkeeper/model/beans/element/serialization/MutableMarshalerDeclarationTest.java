package xyz.cloudkeeper.model.beans.element.serialization;

import cloudkeeper.serialization.StringMarshaler;
import org.testng.Assert;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import xyz.cloudkeeper.model.beans.MutableLocatableContract;
import xyz.cloudkeeper.model.beans.StandardCopyOption;

public class MutableMarshalerDeclarationTest {
    @Factory
    public Object[] contracts() {
        return MutableLocatableContract.contractsFor(MutableSerializationDeclaration.class);
    }

    @Test
    public void fromClass() {
        MutableSerializationDeclaration actual
            = MutableSerializationDeclaration.fromClass(StringMarshaler.class, StandardCopyOption.STRIP_LOCATION);
        MutableSerializationDeclaration expected = new MutableSerializationDeclaration()
            .setSimpleName(StringMarshaler.class.getSimpleName());
        Assert.assertEquals(actual, expected);
    }
}
