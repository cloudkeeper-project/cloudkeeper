package com.svbio.cloudkeeper.maven;

import com.svbio.cloudkeeper.model.immutable.element.Version;
import org.eclipse.aether.artifact.Artifact;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.URI;

public class BundlesTest {
    @Test
    public void testBundleIdentifierFromMaven() {
        URI uri = Bundles.bundleIdentifierFromMaven("foo", "bar", Version.valueOf("1.0"));
        Assert.assertEquals(uri.getScheme(), Bundles.URI_SCHEME);
        Assert.assertEquals(uri.getSchemeSpecificPart(), "foo:bar:ckbundle:1.0");
    }

    @Test
    public void testUnresolvedArtifactFromURI() {
        URI uri = Bundles.bundleIdentifierFromMaven("foo", "bar", Version.valueOf("1.0"));
        Artifact artifact = Bundles.unresolvedArtifactFromURI(uri);
        Assert.assertEquals(artifact.getGroupId(), "foo");
        Assert.assertEquals(artifact.getArtifactId(), "bar");
        Assert.assertEquals(artifact.getVersion(), "1.0");
        Assert.assertNull(artifact.getFile());

        try {
            Bundles.unresolvedArtifactFromURI(URI.create("http://127.0.0.1/"));
            Assert.fail();
        } catch (IllegalArgumentException exception) {
            Assert.assertTrue(exception.getMessage().contains(Bundles.URI_SCHEME)
                && exception.getMessage().contains(Bundles.ARTIFACT_TYPE));
        }
    }
}
