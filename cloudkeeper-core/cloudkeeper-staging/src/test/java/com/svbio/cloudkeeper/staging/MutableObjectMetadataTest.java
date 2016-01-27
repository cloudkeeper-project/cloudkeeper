package com.svbio.cloudkeeper.staging;

import cloudkeeper.serialization.IntegerMarshaler;
import cloudkeeper.serialization.SerializableMarshaler;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.annotation.Nullable;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.util.Arrays;

public class MutableObjectMetadataTest {
    @Nullable private Marshaller marshaller;
    @Nullable private Unmarshaller unmarshaller;

    @BeforeTest
    public void setup() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(MutableObjectMetadata.class);
        marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        unmarshaller = jaxbContext.createUnmarshaller();
        unmarshaller.setEventHandler(new DefaultValidationEventHandler());
    }

    @Test
    public void xmlSerialization() throws JAXBException {
        assert marshaller != null && unmarshaller != null;

        MutableObjectMetadata original = new MutableObjectMetadata()
            .setMarshalers(Arrays.asList(
                new MutableMarshalerIdentifier()
                    .setName(IntegerMarshaler.class.getName())
                    .setBundleIdentifier(URI.create("x-test:cloudkeeper.example.bundle:1.2.3")),
                new MutableMarshalerIdentifier()
                    .setName(SerializableMarshaler.class.getName())
                    .setBundleIdentifier(URI.create("x-test:cloudkeeper.other.bundle:3.1.4-SNAPSHOT"))
            ));

        StringWriter stringWriter = new StringWriter();
        marshaller.marshal(original, stringWriter);
        StringReader stringReader = new StringReader(stringWriter.toString());
        Object deserialized = unmarshaller.unmarshal(stringReader);

        Assert.assertEquals(original, deserialized);
        Assert.assertEquals(original.hashCode(), deserialized.hashCode());
    }
}
