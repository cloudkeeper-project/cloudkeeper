package com.svbio.cloudkeeper.maven;

import com.svbio.cloudkeeper.model.beans.element.MutableBundle;
import com.svbio.cloudkeeper.model.immutable.element.Version;
import com.svbio.cloudkeeper.model.util.SourceCodeLocationListener;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

import javax.annotation.Nullable;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

public final class Bundles {
    public static final String URI_SCHEME = "x-maven";
    public static final String ARTIFACT_TYPE = "ckbundle";

    private Bundles() {
        throw new AssertionError(String.format("No %s instances for you!", getClass().getName()));
    }

    /**
     * Returns the bundle identifier corresponding to the Maven CloudKeeper-bundle artifact with the given group ID,
     * artifact ID, and version.
     *
     * <p>According to {@link DefaultArtifact#DefaultArtifact(String)}, the String
     * representation of Maven coordinates is {@code <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>}.
     * The Maven bundle loader uses the same for its URIs.
     *
     * @param groupId group ID of Maven artifact
     * @param artifactId artifact ID of Maven artifact
     * @param version version of Maven artifact
     * @return bundle identifier
     */
    public static URI bundleIdentifierFromMaven(String groupId, String artifactId, Version version) {
        Objects.requireNonNull(groupId);
        Objects.requireNonNull(artifactId);
        Objects.requireNonNull(version);

        try {
            return new URI(URI_SCHEME, groupId + ':' + artifactId + ':' + ARTIFACT_TYPE + ':' + version, null);
        } catch (URISyntaxException exception) {
            throw new IllegalStateException(String.format(
                "Unexpected exception while trying to create URI '%s:%s:%s:%s:%s'.",
                URI_SCHEME, groupId, artifactId, ARTIFACT_TYPE, version
            ), exception);
        }
    }

    /**
     * Returns an (unresolved) {@link Artifact} corresponding the the CloudKeeper bundle identifier.
     *
     * @param bundleIdentifier CloudKeeper bundle identifier
     * @return {@link Artifact} corresponding the the CloudKeeper bundle identifier
     * @throws IllegalArgumentException if the schema-specific part of the URI is not of form
     *     {@code x-maven:<groupId>:<artifactId>:ckbundle[:<classifier>]:<version>}
     */
    public static Artifact unresolvedArtifactFromURI(URI bundleIdentifier) {
        @Nullable String coordinates = bundleIdentifier.getSchemeSpecificPart();
        if (URI_SCHEME.equals(bundleIdentifier.getScheme()) && coordinates != null && !coordinates.isEmpty()) {
            DefaultArtifact artifact = new DefaultArtifact(coordinates);
            if (ARTIFACT_TYPE.equals(artifact.getExtension())) {
                return artifact;
            }
        }

        throw new IllegalArgumentException(String.format(
            "Expected URI of form '%s:<groupId>:<artifactId>:%s[:<classifier>]:<version>', but got '%s'.",
            URI_SCHEME, ARTIFACT_TYPE, bundleIdentifier
        ));
    }

    /**
     * Returns a new {@link MutableBundle} instance, deserialized from the given (resolved) artifact.
     *
     * <p>Each {@link MutableBundle} will have a bundle identifier returned from
     * {@link #bundleIdentifierFromMaven(String, String, Version)}. The bundle identifier is inferred
     * from the Maven artifact, it is not part of the generated bundle XML.
     *
     * @param jaxbContext The JAXB context. This should be the result of {@link JAXBContext#newInstance(Class[])} where
     *     {@code MutableBundle.class} was passed as argument.
     * @param xmlInputFactory XML input factory. This is typically the result of {@link XMLInputFactory#newFactory()}.
     * @param bundleArtifact the (resolved) artifact (of type {@link #ARTIFACT_TYPE}) that should be loaded
     * @return the CloudKeeper bundle corresponding to the artifact
     * @throws NullPointerException if an argument is null
     * @throws IllegalArgumentException if an argument violates the constraints given above
     */
    public static MutableBundle loadBundle(JAXBContext jaxbContext, XMLInputFactory xmlInputFactory,
            Artifact bundleArtifact) throws JAXBException, XMLStreamException {
        Objects.requireNonNull(jaxbContext);
        Objects.requireNonNull(xmlInputFactory);
        Objects.requireNonNull(bundleArtifact);
        @Nullable File file = bundleArtifact.getFile();
        if (file == null || !ARTIFACT_TYPE.equals(bundleArtifact.getExtension())) {
            throw new IllegalArgumentException(String.format(
                "Expected resolved artifact of type %s, but got %s.", ARTIFACT_TYPE, bundleArtifact
            ));
        }

        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        StreamSource source = new StreamSource(file);
        XMLStreamReader streamReader = xmlInputFactory.createXMLStreamReader(source);
        try {
            unmarshaller.setListener(new SourceCodeLocationListener(streamReader));
            MutableBundle dependencyBundle = (MutableBundle) unmarshaller.unmarshal(streamReader);
            dependencyBundle.setBundleIdentifier(bundleIdentifierFromMaven(
                bundleArtifact.getGroupId(),
                bundleArtifact.getArtifactId(),
                Version.valueOf(bundleArtifact.getVersion())
            ));
            return dependencyBundle;
        } finally {
            streamReader.close();
        }
    }
}
