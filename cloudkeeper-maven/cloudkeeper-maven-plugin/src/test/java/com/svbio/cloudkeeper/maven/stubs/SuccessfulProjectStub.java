package com.svbio.cloudkeeper.maven.stubs;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;

public final class SuccessfulProjectStub extends AbstractProjectStub {
    public SuccessfulProjectStub() throws IOException, XmlPullParserException {
        super("successful.pom.xml");
    }
}
