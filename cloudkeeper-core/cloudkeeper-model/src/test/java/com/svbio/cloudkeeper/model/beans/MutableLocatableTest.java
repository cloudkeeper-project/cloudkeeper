package com.svbio.cloudkeeper.model.beans;

import com.svbio.cloudkeeper.model.beans.execution.MutableExecutable;
import com.svbio.cloudkeeper.model.immutable.Location;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;

/**
 * This test verifies that JAXB is able to produce correct line numbers when unmarshalling XML.
 */
public class MutableLocatableTest {
    static class SourceCodeLocationListener extends Unmarshaller.Listener {
        private final XMLStreamReader xmlStreamReader;

        SourceCodeLocationListener(XMLStreamReader xmlStreamReader) {
            this.xmlStreamReader = xmlStreamReader;
        }

        @Override
        public void beforeUnmarshal(Object target, Object parent) {
            if (target instanceof MutableLocatable) {
                javax.xml.stream.Location location = xmlStreamReader.getLocation();
                ((MutableLocatable<?>) target).setLocation(
                    new Location(location.getSystemId(), location.getLineNumber(), location.getColumnNumber())
                );
            }
        }
    }

    @Test
    public void locationTest() throws XMLStreamException, JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(MutableExecutable.class);

        XMLInputFactory inputFactory = XMLInputFactory.newFactory();
        StreamSource source = new StreamSource(getClass().getResourceAsStream("MutableLocatableTest.xml"),
            "file:/tmp/MutableLocatableTest.xml");
        XMLStreamReader streamReader = inputFactory.createXMLStreamReader(source);

        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        unmarshaller.setListener(new SourceCodeLocationListener(streamReader));
        MutableExecutable executable = (MutableExecutable) unmarshaller.unmarshal(streamReader);

        Location location;

        location = executable.getLocation();
        Assert.assertEquals(location.getSystemId(), "file:/tmp/MutableLocatableTest.xml");
        Assert.assertEquals(location.getLineNumber(), 2);
        Assert.assertTrue(location.getColumnNumber() >= 59 && location.getColumnNumber() <= 60);

        location = executable.getModule().getLocation();
        Assert.assertEquals(location.getSystemId(), "file:/tmp/MutableLocatableTest.xml");
        Assert.assertEquals(location.getLineNumber(), 7);
        Assert.assertTrue(location.getColumnNumber() >= 33 && location.getColumnNumber() <= 34);
    }
}
