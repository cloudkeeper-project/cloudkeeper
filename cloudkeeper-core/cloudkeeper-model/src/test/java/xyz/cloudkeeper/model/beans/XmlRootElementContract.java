package xyz.cloudkeeper.model.beans;

import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.testng.Assert;
import org.testng.ITest;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Objects;

public class XmlRootElementContract implements ITest {
    private final Object instance;
    private final Class<?> clazz;

    public XmlRootElementContract(Object instance) {
        Objects.requireNonNull(instance);
        this.instance = instance;
        clazz = instance.getClass();
    }

    @Override
    public String getTestName() {
        return clazz.getSimpleName();
    }

    @Test
    public void jaxbMarshallingUnmarshalling() throws Exception {
        // We use EclipseLink MOXy for marshalling, and the JAXB Reference Implementation for unmarshalling
        JAXBContext moxyContext = JAXBContextFactory.createContext(new Class<?>[]{clazz}, null);
        Marshaller marshaller = moxyContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        JAXBContext internalJaxbContext = JAXBContext.newInstance(clazz);
        Unmarshaller unmarshaller = internalJaxbContext.createUnmarshaller();

        StringWriter stringWriter = new StringWriter();
        marshaller.marshal(instance, stringWriter);
        Object deserializedObject;
        try (StringReader stringReader = new StringReader(stringWriter.toString())) {
            deserializedObject = unmarshaller.unmarshal(new StreamSource(stringReader), clazz).getValue();
        }

        Assert.assertEquals(deserializedObject, instance);
        Assert.assertEquals(deserializedObject.hashCode(), instance.hashCode());
    }
}
