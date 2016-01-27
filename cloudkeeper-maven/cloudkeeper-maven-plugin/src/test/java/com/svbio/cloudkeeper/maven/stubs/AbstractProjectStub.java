package com.svbio.cloudkeeper.maven.stubs;

import com.svbio.cloudkeeper.maven.ITCompileBundleMojo;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.testing.stubs.ArtifactStub;
import org.apache.maven.plugin.testing.stubs.DefaultArtifactHandlerStub;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.util.artifact.JavaScopes;

import java.io.IOException;
import java.util.List;

abstract class AbstractProjectStub extends MavenProjectStub {
    AbstractProjectStub(String pomResourceName) throws IOException, XmlPullParserException {
        super(new MavenXpp3Reader().read(ITCompileBundleMojo.class.getResourceAsStream(pomResourceName)));

        Model model = getModel();
        setGroupId(model.getGroupId());
        setArtifactId(model.getArtifactId());
        setVersion(model.getVersion());
        setName(model.getName());
        setUrl(model.getUrl());
        setPackaging(model.getPackaging());

        SimpleArtifactStub artifact = new SimpleArtifactStub(
            model.getGroupId(), model.getArtifactId(), model.getVersion(), model.getPackaging());
        artifact.setArtifactHandler(new SimpleArtifactHandlerStub(model.getPackaging()));
        setArtifact(artifact);

        Build build = new Build();
        build.setFinalName(model.getArtifactId() + '-' + model.getVersion());
        setBuild(build);

        for (Dependency dependency: model.getDependencies()) {
            if (dependency.getScope() == null) {
                dependency.setScope(JavaScopes.COMPILE);
            }
        }
    }

    @Override
    public List<Dependency> getDependencies() {
        return getModel().getDependencies();
    }

    private static final class SimpleArtifactStub extends ArtifactStub {
        private final VersionRange versionRange;
        private ArtifactHandler artifactHandler;

        private SimpleArtifactStub(String groupId, String artifactId, String version, String packaging) {
            setGroupId(groupId);
            setArtifactId(artifactId);
            setVersion(version);
            setType(packaging);
            versionRange = VersionRange.createFromVersion(version);
        }

        @Override
        public ArtifactHandler getArtifactHandler() {
            return artifactHandler;
        }

        @Override
        public void setArtifactHandler(ArtifactHandler artifactHandler) {
            this.artifactHandler = artifactHandler;
        }

        @Override
        public VersionRange getVersionRange() {
            return versionRange;
        }
    }

    private static final class SimpleArtifactHandlerStub extends DefaultArtifactHandlerStub {
        private SimpleArtifactHandlerStub(String type) {
            super(type);
            setLanguage("java");
        }
    }
}
