package com.svbio.cloudkeeper.marshaling;

import cloudkeeper.types.ByteSequence;
import com.svbio.cloudkeeper.model.api.Marshaler;
import com.svbio.cloudkeeper.model.api.MarshalingException;
import com.svbio.cloudkeeper.model.api.UnmarshalContext;
import com.svbio.cloudkeeper.model.immutable.element.Key;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Objects;

/**
 * Unmarshal context that recursively creates sub-contexts when {@link UnmarshalContext#readObject(Key)} is called
 * and that delegates elementary unmarshal operations to a {@link UnmarshalSource} instance.
 */
public final class DelegatingUnmarshalContext implements UnmarshalContext {
    private final UnmarshalSource unmarshalSource;
    private final ClassLoader classLoader;

    private DelegatingUnmarshalContext(UnmarshalSource unmarshalSource, ClassLoader classLoader) {
        this.unmarshalSource = unmarshalSource;
        this.classLoader = classLoader;
    }

    /**
     * Creates a new unmarshal context that delegates elementary unmarshal operations to the given source.
     *
     * <p>In most cases, {@link #unmarshal(UnmarshalSource, ClassLoader)} is the more appropriate high-level alternative
     * to instantiating an unmarshal context explicitly.
     *
     * @param unmarshalSource unmarshal source
     * @param classLoader class loader that marshalers can retrieve using {@link UnmarshalContext#getClassLoader()}
     * @return the new unmarshal context
     */
    public static DelegatingUnmarshalContext create(UnmarshalSource unmarshalSource, ClassLoader classLoader) {
        Objects.requireNonNull(unmarshalSource);
        Objects.requireNonNull(classLoader);
        return new DelegatingUnmarshalContext(unmarshalSource, classLoader);
    }

    /**
     * Unmarshals an object from the given source.
     *
     * <p>This method is equivalent to creating an unmarshal context using {@link #create(UnmarshalSource, ClassLoader)}
     * and then calling {@link #process()}.
     *
     * @param unmarshalSource unmarshal source
     * @param classLoader class loader to be returned by {@link UnmarshalContext#getClassLoader()}
     * @return the object
     * @throws MarshalingException if a deserialization error occurs
     * @throws IOException if an I/O error occurs
     */
    public static Object unmarshal(UnmarshalSource unmarshalSource, ClassLoader classLoader) throws IOException {
        Objects.requireNonNull(unmarshalSource);
        Objects.requireNonNull(classLoader);
        return new DelegatingUnmarshalContext(unmarshalSource, classLoader).process();
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public ByteSequence getByteSequence(Key key) throws IOException {
        Objects.requireNonNull(key);
        @Nullable ByteSequence byteSequence = unmarshalSource.getByteSequence(key);
        if (byteSequence == null) {
            throw new MarshalingException(String.format("Could not find byte-sequence node with key '%s'.", key));
        }
        return byteSequence;
    }

    @Override
    public Object readObject(Key key) throws IOException {
        Objects.requireNonNull(key);

        @Nullable UnmarshalSource childUnmarshalSource = unmarshalSource.resolve(key);
        if (childUnmarshalSource == null) {
            throw new MarshalingException(String.format("Could not find internal tree node with key '%s'.", key));
        }

        DelegatingUnmarshalContext childContext = new DelegatingUnmarshalContext(childUnmarshalSource, classLoader);
        return childContext.process();
    }

    /**
     * Reads the object from the current unmarshal context.
     *
     * <p>This method is unlike {@link #readObject(Key)}, which creates a new child unmarshal context. This method
     * instead uses the current context to unmarshal the object, using the {@link UnmarshalSource} instance specified at
     * construction time.
     *
     * @throws MarshalingException if unmarshaling the given object fails (for instance, when a user-defined
     *     {@link Marshaler#get(UnmarshalContext)} implementation throws or causes a {@link MarshalingException})
     * @throws IOException if the operation failed because of an I/O error
     */
    public Object process() throws IOException {
        @Nullable Object object = unmarshalSource.getObject();
        if (object != null) {
            return object;
        }

        return unmarshalSource.getMarshaler().get(this);
    }
}
