package cloudkeeper.serialization;

import xyz.cloudkeeper.model.CloudKeeperSerialization;
import xyz.cloudkeeper.model.api.MarshalContext;
import xyz.cloudkeeper.model.api.Marshaler;
import xyz.cloudkeeper.model.api.UnmarshalContext;
import xyz.cloudkeeper.model.immutable.element.Index;
import xyz.cloudkeeper.model.immutable.element.SimpleName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * CloudKeeper serialization class for {@link Collection} instances.
 *
 * <p>This serialization plug-in serializes collections by serializing each element individually.
 */
@CloudKeeperSerialization(IntegerMarshaler.class)
public final class CollectionMarshaler implements Marshaler<Collection<?>> {
    private static final SimpleName SIZE = SimpleName.identifier("size");

    @Override
    public boolean isImmutable(Collection<?> object) {
        return false;
    }

    @Override
    public void put(Collection<?> collection, MarshalContext context) throws IOException {
        int count = 0;
        context.writeObject(collection.size(), SIZE);
        for (Object object: collection) {
            context.writeObject(object, Index.index(count));
            ++count;
        }
    }

    @Override
    public Collection<?> get(UnmarshalContext context) throws IOException {
        int size = (int) context.readObject(SIZE);
        List<Object> list = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) {
            list.add(context.readObject(Index.index(i)));
        }
        return list;
    }
}
