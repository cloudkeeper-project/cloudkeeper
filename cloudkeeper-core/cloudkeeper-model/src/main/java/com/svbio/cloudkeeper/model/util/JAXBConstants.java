package com.svbio.cloudkeeper.model.util;

import javax.xml.bind.annotation.XmlSchema;

public final class JAXBConstants {
    private JAXBConstants() {
        throw new AssertionError(String.format("No %s instances for you!", getClass().getName()));
    }

    public static final String NAMESPACE = "http://www.svbio.com/cloudkeeper/1.0.0";

    // "http://www.florian-schoppmann.net/cloudkeeper-v1_0_0.xsd";
    public static final String LOCATION = XmlSchema.NO_LOCATION;
}
