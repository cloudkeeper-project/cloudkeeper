package cloudkeeper.types;

import cloudkeeper.serialization.ByteSequenceMarshaler;
import xyz.cloudkeeper.model.CloudKeeperSerialization;
import xyz.cloudkeeper.model.api.Marshaler;
import xyz.cloudkeeper.model.api.UnmarshalContext;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Byte sequence.
 *
 * <p>Instances of this interface represent arbitrary immutable byte sequences. For technical reasons, this interface
 * keeps several properties unspecified. Implementations may implicitly or explicitly contain external references (for
 * instance, a URI to some remote storage). Therefore, it may be impossible for implementations to enforce immutability.
 * However, the effects of modifying a byte sequence or the externally referenced data are undefined, and must be
 * avoided in portable code! Since byte sequences may be of arbitrary size, this interface does not define a particular
 * behavior for {@link Object#equals(Object)} or {@link Object#hashCode()}.
 *
 * <p>The default media type is {@link #DEFAULT_CONTENT_TYPE}, even though classes implementing this interface may
 * have a more specific type.
 *
 * <p>All implementations of this interface are required to support CloudKeeper serialization with
 * {@link ByteSequenceMarshaler}. To this end, method {@link #getDecorator()} must provide an appropriate decorator
 * that takes a raw byte sequence and returns an appropriately typed instance of a subclass.
 */
@CloudKeeperSerialization(ByteSequenceMarshaler.class)
public interface ByteSequence {
    /**
     * The default media type {@code application/octet-stream}.
     */
    String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    /**
     * Returns a decorator capable of reconstructing a properly typed byte sequence, given a raw byte sequence.
     *
     * <p>The decorator class is retrieved and stored during serialization, so that upon deserialization in method
     * {@link ByteSequenceMarshaler#get(UnmarshalContext)}, the decorator
     * class can be used to return a properly typed new instance of this interface.
     *
     * @return the decorator appropriate for this instance
     */
    ByteSequenceMarshaler.Decorator getDecorator();

    /**
     * Returns the absolute URI that identifies the byte sequence represented by this object, or {@code null} if no URI
     * is available.
     *
     * <p>Implementations may provide the absolute URI for optimization purposes, but are not required to. If the URI is
     * not provided and {@code null} is returned, callers must process the byte sequence through the input stream
     * returned by {@link #newInputStream()} instead. Callers relying on the URI for anything but optimization risk
     * being non-portable. Correspondingly, the URI is purely metadata and not part of the <em>value</em> represented by
     * an instance. In particular, the URI will be different in different CloudKeeper module invocations.
     *
     * <p>The effects of directly <em>modifying</em> the byte sequence referenced by the returned URI are undefined.
     * Note that the URI might, for instance, identify a resource inside the CloudKeeper staging area, and modifications
     * could impact a workflow execution or even lead to invalid results. For this reason, a staging area may be
     * configured to never reveal URIs, and callers should anticipate that this methods may return {@code null}.
     *
     * @return the absolute URI that identifies the byte sequence represented by this object, or {@code null} if no URI
     *     is available
     */
    @Nullable
    URI getURI();

    /**
     * Returns whether this byte sequence is self-contained.
     *
     * <p>A byte sequence is <em>self-contained</em> if all transitively referenced external data (files, URLs, etc.) is
     * guaranteed to never change during the lifetime of the current JVM.
     *
     * @return {@code true} if this byte sequence is self-contained, {@code false} otherwise
     *
     * @see Marshaler#isImmutable(Object)
     */
    boolean isSelfContained();

    /**
     * Returns the length of this byte sequence.
     *
     * @return the content length of the byte sequence, guaranteed ≥ 0
     * @throws IOException if an I/O error occurs
     */
    long getContentLength() throws IOException;

    /**
     * Returns the media type of this byte sequence.
     *
     * @return the content type of the byte sequence, guaranteed non-null
     * @throws IOException if an I/O error occurs
     */
    String getContentType() throws IOException;

    /**
     * Returns a new {@link InputStream} for reading the byte sequence.
     *
     * <p>This method may be called multiple times. The caller is always responsible for closing the returned stream.
     * Therefore, this method should only be called in the header of a try-with-resources statement.
     *
     * @return The {@link InputStream} for reading this byte sequence. No a-priori assumptions should be made whether
     *     the stream is buffered.
     * @throws IOException if an I/O error occurs
     */
    InputStream newInputStream() throws IOException;

    /**
     * Returns a byte array with with the content of this byte sequence.
     *
     * @return byte array
     * @throws IOException if an I/O error occurs
     */
    default byte[] toByteArray() throws IOException {
        // Same size as the buffer used by java.nio.file.Files#read
        final int bufferSize = 8192;
        try (
            InputStream inputStream = newInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(bufferSize)
        ) {
            byte[] buffer = new byte[bufferSize];
            while (true) {
                int numBytesRead = inputStream.read(buffer);
                if (numBytesRead == -1) {
                    break;
                }
                outputStream.write(buffer, 0, numBytesRead);
            }

            // From the ByteArrayOutputStream JavaDoc: The methods [...] can be called after the stream has been
            // closed without generating an IOException. Closing streams is idempotent, so there will not be a
            // problem at the end of the try-with-resources statement.
            outputStream.close();
            return outputStream.toByteArray();
        }
    }
}
