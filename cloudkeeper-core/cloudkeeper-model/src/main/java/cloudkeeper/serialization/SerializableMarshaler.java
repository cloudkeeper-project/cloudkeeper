package cloudkeeper.serialization;

import cloudkeeper.types.ByteSequence;
import xyz.cloudkeeper.model.api.MarshalContext;
import xyz.cloudkeeper.model.api.Marshaler;
import xyz.cloudkeeper.model.api.MarshalingException;
import xyz.cloudkeeper.model.api.UnmarshalContext;
import xyz.cloudkeeper.model.immutable.element.NoKey;
import xyz.cloudkeeper.model.util.ClassLoadingObjectInputStream;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * CloudKeeper serialization that serializes objects that implement the {@link Serializable} interface.
 *
 * <p>This CloudKeeper serialization serializes objects into a single byte stream using
 * {@link ObjectOutputStream#writeObject(Object)}. Likewise, deserialization happens with
 * {@link ObjectInputStream#readObject()}.
 */
public final class SerializableMarshaler implements Marshaler<Serializable> {
    @Override
    public boolean isImmutable(Serializable object) {
        return false;
    }

    @Override
    public void put(Serializable object, MarshalContext context) throws IOException {
        try (ObjectOutputStream objectOutputStream
                = new ObjectOutputStream(context.newOutputStream(NoKey.instance()))) {
            objectOutputStream.writeObject(object);
        }
    }

    @Override
    public Serializable get(UnmarshalContext context) throws IOException {
        ByteSequence byteSequence = context.getByteSequence(NoKey.instance());
        try (ObjectInputStream objectInputStream
                = new ClassLoadingObjectInputStream(byteSequence.newInputStream(), context.getClassLoader())) {
            return (Serializable) objectInputStream.readObject();
        } catch (ClassNotFoundException exception) {
            throw new MarshalingException(exception);
        }
    }
}
