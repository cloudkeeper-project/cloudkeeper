package com.svbio.cloudkeeper.model.util;

import com.svbio.cloudkeeper.model.beans.MutableLocatable;
import com.svbio.cloudkeeper.model.immutable.Location;

import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamReader;

/**
 * Simple JAXB unmarshaller listener that updates the source-code location in {@link MutableLocatable} instances.
 */
public final class SourceCodeLocationListener extends Unmarshaller.Listener {
    private final XMLStreamReader xmlStreamReader;

    /**
     * Constructor.
     *
     * @param xmlStreamReader the XML stream reader whose {@link XMLStreamReader#getLocation()} method will be queried
     *     during {@link #beforeUnmarshal(Object, Object)} events
     */
    public SourceCodeLocationListener(XMLStreamReader xmlStreamReader) {
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
