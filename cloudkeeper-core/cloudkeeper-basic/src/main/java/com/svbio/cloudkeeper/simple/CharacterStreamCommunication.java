package com.svbio.cloudkeeper.simple;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * This class consists of static methods and classes for writing and reading character streams that have an embedded
 * {@link Serializable} instance.
 */
public final class CharacterStreamCommunication {
    private static final String BOUNDARY = "X-Object-Boundary";
    private static final String CONTENT_LENGTH = "Content-Length";
    private static final String CONTENT_MD5 = "Content-MD5";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
    private static final String JAVA_SERIALIZED_OBJECT = "application/x-java-serialized-object";
    private static final String BASE_64 = "base64";

    private CharacterStreamCommunication() {
        throw new AssertionError(String.format("No %s instances for you!", getClass().getName()));
    }

    /**
     * @see <a href="http://tools.ietf.org/html/rfc2046#section-5.1.1">RFC 2046</a>
     */
    private static final Pattern BOUNDARY_PATTERN
        = Pattern.compile("[\\p{Alnum}'()+_,-./:=? ]{0,69}[\\p{Alnum}'()+_,-./:=?]");

    /**
     * Writes the given boundary string to the given destination.
     *
     * <p>This method will write a line of form {@code X-Object-Boundary: <boundary>}.
     *
     * @param boundary Boundary that is valid according to
     *     <a href="http://tools.ietf.org/html/rfc2046#section-5.1.1">RFC 2046</a>. This boundary must also be passed to
     *     {@link #writeObject(Serializable, Appendable, String)}.
     * @param destination destination to write the boundary line to
     * @throws IllegalArgumentException if the given boundary is is not a valid boundary according to RFC 2046
     * @throws IOException if a serialization or I/O error occurs
     */
    public static void writeBoundary(String boundary, Appendable destination) throws IOException {
        Objects.requireNonNull(boundary);
        if (!BOUNDARY_PATTERN.matcher(boundary).matches()) {
            throw new IllegalArgumentException(String.format(
                "Expected boundary that is valid according to RFC 2046, but got '%s'.", boundary
            ));
        }
        destination.append(BOUNDARY).append(": ").append(boundary).append('\n');
    }

    /**
     * Writes the given {@link Serializable} object to the given destination.
     *
     * <p>This method serializes the given object using {@link ObjectOutputStream}. It then encodes the object and
     * writes out a representation compatible with <a href="http://www.ietf.org/rfc/rfc2045.txt">RFC 2045</a>: It first
     * writes the boundary (of form {@code "--<boundary>"}), followed by {@code Content-Length}, {@code Content-MD5},
     * {@code Content-Type}, and {@code Content-Transfer-Encoding} headers. Then it writes the content in base64-encoded
     * form (using {@link Base64#getMimeEncoder()}), followed by the boundary again (this time of form
     * {@code "--<boundary>--"}).
     *
     * <p>It is expected that the entire destination is read on the receiving side using {@link Splitter}.
     *
     * @param serializable object to serialize
     * @param destination destination for the serialized and encoded output
     * @throws IOException if a serialization or I/O error occurs
     *
     * @see Splitter#Splitter(Class, BufferedReader)
     * @see Base64#getMimeEncoder()
     */
    public static void writeObject(Serializable serializable, Appendable destination, String boundary)
            throws IOException {
        try (
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)
        ) {
            objectOutputStream.writeObject(serializable);
            byte[] bytes = byteArrayOutputStream.toByteArray();
            MessageDigest md = createMD5Digest();
            byte[] md5 = md.digest(bytes);

            destination.append("--").append(boundary).append('\n');
            destination.append(CONTENT_LENGTH).append(": ").append(String.valueOf(bytes.length)).append('\n');
            destination.append(CONTENT_MD5).append(": ").append(Base64.getEncoder().encodeToString(md5)).append('\n');
            destination.append(CONTENT_TYPE).append(": ").append(JAVA_SERIALIZED_OBJECT).append('\n');
            destination.append(CONTENT_TRANSFER_ENCODING).append(": ").append(BASE_64).append("\n\n");
            destination.append(Base64.getMimeEncoder().encodeToString(bytes)).append('\n');
            destination.append("--").append(boundary).append("--").append('\n');
        }
    }

    /**
     * Class for reading a character stream <em>and</em> a {@link Serializable} instance encoded with that stream.
     *
     * <p>This class wraps a {@link BufferedReader} and, unlike that class, only provides the high-level method
     * {@link #readLine()} for reading lines from the character stream. While doing so, the embedded
     * {@link Serializable} instance is automatically extracted, while at the same time all lines used for encoding the
     * embedded instance are hidden. The instance is available after reading the instance till the end, using
     * {@link #getResult()}.
     *
     * @param <T> type of the {@link Serializable} encoded in the character stream
     */
    public static final class Splitter<T extends Serializable> implements Closeable {
        private static final int MAX_QUOTED_STRING_LENGTH = 32;

        private enum State {
            INITIALIZED,
            AFTER_START_BOUNDARY,
            AFTER_OBJECT,
            AFTER_END_BOUNDARY,
            ENDED
        }

        private final Class<T> clazz;
        private final BufferedReader bufferedReader;
        @Nullable private String comparison = BOUNDARY + ':';
        @Nullable private String boundary;
        private State state = State.INITIALIZED;
        @Nullable private T result = null;

        /**
         * Creates a splitter that uses the given {@link BufferedReader} instances as source.
         *
         * <p>Closing the newly constructed instance will close the reader.
         *
         * @param clazz class of the embedded instance
         * @param bufferedReader a reader
         */
        public Splitter(Class<T> clazz, BufferedReader bufferedReader) {
            this.clazz = clazz;
            this.bufferedReader = bufferedReader;
        }

        @Override
        public void close() throws IOException {
            bufferedReader.close();
        }

        /**
         * Reads a line of text.  A line is considered to be terminated by any one of a line feed ('\n'), a carriage
         * return ('\r'), or a carriage return followed immediately by a linefeed.
         *
         * @return A String containing the contents of the line, not including any line-termination characters, or null
         *     if the end of the stream has been reached
         * @throws  IOException If an I/O error occurs
         *
         * @see BufferedReader#readLine()
         */
        public String readLine() throws IOException {
            while (state != State.ENDED) {
                @Nullable String nextLine = bufferedReader.readLine();
                if (nextLine == null) {
                    state = State.ENDED;
                }

                switch (state) {
                    case INITIALIZED:
                        if (nextLine.startsWith(BOUNDARY + ':')) {
                            boundary = nextLine.substring(BOUNDARY.length() + 1).trim();
                            comparison = "--" + boundary;
                            state = State.AFTER_START_BOUNDARY;
                            continue;
                        }
                        break;
                    case AFTER_START_BOUNDARY:
                        if (nextLine.equals(comparison)) {
                            result = readObject(clazz, bufferedReader);
                            comparison = "--" + boundary + "--";
                            state = State.AFTER_OBJECT;
                            continue;
                        }
                        break;
                    case AFTER_OBJECT:
                        if (nextLine.equals(comparison)) {
                            comparison = null;
                            state = State.AFTER_END_BOUNDARY;
                            continue;
                        } else {
                            throw new IOException(String.format(
                                "Expected end-boundary '%s', but found line that starts with '%s'.",
                                comparison, nextLine.substring(0, Math.max(MAX_QUOTED_STRING_LENGTH, nextLine.length()))
                            ));
                        }
                    default:
                }
                return nextLine;
            }
            return null;
        }

        public void consumeAll() throws IOException {
            while (state != State.ENDED) {
                readLine();
            }
        }

        public T getResult() throws IOException {
            if (state != State.ENDED) {
                throw new IllegalStateException("getResult() called before reader has been entirely consumed.");
            } else if (result == null) {
                throw new IOException("Reached end of stream before object was read successfully.");
            } else {
                return result;
            }
        }
    }

    /**
     * Reads (and returns) a {@link Serializable} object from the given {@link BufferedReader}.
     *
     * <p>This method is meant to be used on the receiving side of a channel that was previously written to using method
     * {@link #writeObject(Serializable, Appendable, String)}. While MIME headers are expected, only the combination of
     * {@code content-type} and {@code content-transfer-encoding} chosen by
     * {@link #writeObject(Serializable, Appendable, String)} is supported by this method. Deviations from this will result in
     * an {@link IOException}.
     *
     * <p>Callers of this method are expected to enclose the output of this method in a proper MIME boundary, as
     * specified by <a href="http://tools.ietf.org/html/rfc2046#section-5.1.1">RFC 2046</a>. Upon succesful return, the
     * reader will be positioned immediately at the beginning of the line after the base64-encoded content.
     *
     * @param clazz class of the object returned by this method
     * @param reader reader that this method will deserialize and decode from
     * @param <T> type of the object returned by this method
     * @return object deserialized and decoded from the given reader
     * @throws IOException if a parsing or I/O error occurs
     *
     * @see #writeObject(Serializable, Appendable, String)
     */
    private static <T extends Serializable> T readObject(Class<T> clazz, BufferedReader reader) throws IOException {
        ByteBuffer byteBuffer = readByteBuffer(reader);
        try (ObjectInputStream objectInputStream = new ObjectInputStream(new ByteBufferInputStream(byteBuffer))) {
            return clazz.cast(objectInputStream.readObject());
        } catch (ClassNotFoundException | ClassCastException exception) {
            throw new IOException(String.format("Invalid serialization of instance of %s.", clazz), exception);
        }
    }

    private static MessageDigest createMD5Digest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                "MD5 algorithm, which is part of Java SE, is not available for unexpected reasons.", exception);
        }
    }

    private static ByteBuffer readByteBuffer(BufferedReader reader) throws IOException {
        Map<String, String> headerMap = parseHeader(reader);
        int expectedLength = requireInt(CONTENT_LENGTH, headerMap);
        String contentType = requireKey(CONTENT_TYPE, headerMap);
        String contentTransferEncoding = requireKey(CONTENT_TRANSFER_ENCODING, headerMap);
        byte[] expectedMD5 = requireBytes(CONTENT_MD5, headerMap);

        if (!JAVA_SERIALIZED_OBJECT.equals(contentType.toLowerCase(Locale.ENGLISH))) {
            throw new IOException(String.format(
                "Expected content type '%s', but found '%s'.", JAVA_SERIALIZED_OBJECT, contentType
            ));
        }
        if (!BASE_64.equals(contentTransferEncoding.toLowerCase(Locale.ENGLISH))) {
            throw new IOException(String.format(
                "Expected content transfer encoding '%s', but found '%s'.", BASE_64, contentTransferEncoding
            ));
        }

        ByteBuffer byteBuffer = ByteBuffer.allocate(expectedLength);
        Base64.Decoder base64Decoder = Base64.getDecoder();
        int length = 0;
        MessageDigest md = createMD5Digest();
        while (length < expectedLength) {
            @Nullable String line = reader.readLine();
            if (line == null) {
                break;
            }

            byte[] decodedBytes;
            try {
                decodedBytes = base64Decoder.decode(line);
            } catch (IllegalArgumentException exception) {
                throw new IOException("Invalid base64 encoding detected.", exception);
            }
            length += decodedBytes.length;
            md.update(decodedBytes);
            byteBuffer.put(decodedBytes);
        }
        byte[] md5 = md.digest();
        if (!Arrays.equals(md5, expectedMD5)) {
            throw new IOException("Invalid MD5 checksum detected.");
        }

        byteBuffer.rewind();
        return byteBuffer;
    }

    private static Map<String, String> parseHeader(BufferedReader reader) throws IOException {
        Map<String, String> map = new LinkedHashMap<>();
        @Nullable String line = reader.readLine();
        while (line != null && !line.isEmpty()) {
            int indexOfColon = line.indexOf(':');
            if (indexOfColon > 0) {
                String header = line.substring(0, indexOfColon);
                String value = line.substring(indexOfColon + 1).trim();
                @Nullable String previous = map.put(header.toLowerCase(Locale.ENGLISH), value);
                if (previous != null) {
                    throw new IOException(String.format(
                        "Duplicate header '%s': Previous value was '%s', current value is '%s'.",
                        header, previous, value
                    ));
                }
            } else {
                throw new IOException("Expected header, but invalid line detected.");
            }
            line = reader.readLine();
        }
        if (line == null) {
            throw new IOException("Stream ended before body started.");
        }

        // Note that we have now: line.isEmpty()
        return map;
    }

    private static String requireKey(String key, Map<String, String> map) throws IOException {
        @Nullable String value = map.get(key.toLowerCase(Locale.ENGLISH));
        if (value == null) {
            throw new IOException(String.format("Missing header '%s'.", key));
        }
        return value;
    }

    private static int requireInt(String key, Map<String, String> map) throws IOException {
        String value = requireKey(key, map);
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new IOException(String.format("Unexpected value for header '%s'.", key), exception);
        }
    }

    private static byte[] requireBytes(String key, Map<String, String> map) throws IOException {
        String value = requireKey(key, map);
        try {
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException exception) {
            throw new IOException(String.format("Invalid base64 encoding of header '%s'.", key), exception);
        }
    }

    private static final class ByteBufferInputStream extends InputStream {
        private static final int BYTE_MASK = 0xFF;
        private final ByteBuffer byteBuffer;

        private ByteBufferInputStream(ByteBuffer byteBuffer) {
            this.byteBuffer = byteBuffer;
        }

        @Override
        public int read() {
            return byteBuffer.hasRemaining()
                ? byteBuffer.get() & BYTE_MASK
                : -1;
        }

        @Override
        public int read(byte[] array, int off, int len) {
            Objects.requireNonNull(array);
            if (off < 0 || len < 0 || len > array.length - off) {
                throw new IndexOutOfBoundsException(String.format(
                    "array.length = %d, offset = %d, length = %d", array.length, off, len
                ));
            } else if (len == 0) {
                return 0;
            }

            int remaining = byteBuffer.remaining();
            int count = remaining >= len
                ? len
                : remaining;
            byteBuffer.get(array, off, count);
            return count;
        }

        @Override
        public int available() throws IOException {
            return byteBuffer.remaining();
        }
    }
}
