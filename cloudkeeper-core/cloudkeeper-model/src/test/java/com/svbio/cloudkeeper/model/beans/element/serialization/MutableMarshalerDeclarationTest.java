package com.svbio.cloudkeeper.model.beans.element.serialization;

import cloudkeeper.serialization.StringMarshaler;
import com.svbio.cloudkeeper.model.beans.MutableLocatableContract;
import com.svbio.cloudkeeper.model.beans.StandardCopyOption;
import org.testng.Assert;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

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
