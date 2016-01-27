package com.svbio.cloudkeeper.s3;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents an S3 key and offers some convenience methods similar to what {@link java.nio.file.Path} offers.
 */
final class S3Path implements Serializable {
    private static final long serialVersionUID = 6444293334743208740L;

    static final char SEPARATOR = '/';

    private final String prefix;
    private final String path;
    private final boolean isFinal;

    private S3Path(String prefix, String path, boolean isFinal) {
        this.prefix = Objects.requireNonNull(prefix);
        // The following line also throws a NullPointerException if path == null.
        if (path.endsWith(String.valueOf(SEPARATOR))) {
            throw new IllegalArgumentException(String.format("Path component must not end with '%s'.", SEPARATOR));
        }
        this.path = path;
        this.isFinal = isFinal;
    }

    public static S3Path empty(String prefix) {
        return new S3Path(prefix, "", false);
    }

    /**
     * Returns this S3 path as string.
     */
    @Override
    public String toString() {
        return prefix + path;
    }

    /**
     * Returns the S3 key prefix component of this S3 key.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Returns the S3 key prefix that all logical children of this S3 path will share.
     *
     * <p>This is always the string representation (consisting of both the prefix and the path components) appended with
     * the separator.
     */
    public String getPrefixForChildren() {
        if (isFinal) {
            throw new IllegalStateException("Tried to get prefix for children of S3 path that is final.");
        }
        return toString() + SEPARATOR;
    }

    /**
     * Returns the key of the S3 object that contains the metadata for this path.
     *
     * <p>This is always the S3 key appended with the separator and {@code serialization.xml}.
     */
    public String getMetadataKey() {
        if (isFinal) {
            throw new IllegalStateException("Tried to get metadata key on S3 path that is final.");
        }
        return prefix + path + SEPARATOR + "meta.xml";
    }

    /**
     * Returns the key of the S3 object that contains the byte stream corresponding to the empty CloudKeeper
     * serialization key.
     */
    public S3Path resolveDefaultKey() {
        return internalResolve(".value", true);
    }

    private S3Path internalResolve(String element, boolean newPathIsFinal) {
        if (element.isEmpty() || element.endsWith(String.valueOf(SEPARATOR))) {
            throw new IllegalArgumentException(String.format("Expected valid path element but got '%s'.", element));
        } else if (isFinal) {
            throw new IllegalStateException(String.format("Cannot resolve '%s' on final S3 Path '%s'.", element, this));
        }

        String newPath = path.isEmpty()
            ? element
            : (path + SEPARATOR + element);
        return new S3Path(prefix, newPath, newPathIsFinal);
    }

    public S3Path resolve(String element) {
        return internalResolve(element, false);
    }
}
