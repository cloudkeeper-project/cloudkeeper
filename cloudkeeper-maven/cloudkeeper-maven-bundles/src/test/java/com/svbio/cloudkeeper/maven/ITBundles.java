package com.svbio.cloudkeeper.maven;

import com.svbio.cloudkeeper.model.api.util.RecursiveDeleteVisitor;
import com.svbio.cloudkeeper.model.beans.StandardCopyOption;
import com.svbio.cloudkeeper.model.beans.SystemBundle;
import com.svbio.cloudkeeper.model.beans.element.MutableBundle;
import com.svbio.cloudkeeper.model.immutable.element.Version;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ITBundles {
    private Path tempDir;

    @BeforeClass
    public void setup() throws IOException {
        tempDir = Files.createTempDirectory(getClass().getSimpleName());
    }

    @AfterClass
    public void tearDown() throws IOException {
        Files.walkFileTree(tempDir, RecursiveDeleteVisitor.getInstance());
    }

    @Test
    public void testLoadBundle() throws JAXBException, XMLStreamException {
        JAXBContext jaxbContext = JAXBContext.newInstance(MutableBundle.class);
        XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();

        Artifact unresolvedArtifact = new DefaultArtifact("foo", "bar", Bundles.ARTIFACT_TYPE, "1.0");
        try {
            Bundles.loadBundle(jaxbContext, xmlInputFactory, unresolvedArtifact);
            Assert.fail("Expected failure without file.");
        } catch (IllegalArgumentException ignored) { }

        MutableBundle systemBundle = SystemBundle.newSystemBundle()
            .setBundleIdentifier(Bundles.bundleIdentifierFromMaven(
                unresolvedArtifact.getGroupId(), unresolvedArtifact.getArtifactId(),
                Version.valueOf(unresolvedArtifact.getVersion())
            ));
        File bundleFile = new File(tempDir.toFile(),"systemBundle.xml");
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(systemBundle, bundleFile);

        Artifact resolvedArtifact = unresolvedArtifact.setFile(bundleFile);
        MutableBundle deserializedBundle = Bundles.loadBundle(jaxbContext, xmlInputFactory, resolvedArtifact);

        Assert.assertEquals(MutableBundle.copyOf(deserializedBundle, StandardCopyOption.STRIP_LOCATION), systemBundle);
    }
}
