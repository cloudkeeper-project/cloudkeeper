package xyz.cloudkeeper.maven.stubs;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;

public final class FailureProjectStub extends AbstractProjectStub {
    public FailureProjectStub() throws IOException, XmlPullParserException {
        super("failure.pom.xml");
    }
}
