package xyz.cloudkeeper.linker.examples;

import xyz.cloudkeeper.model.api.MarshalContext;
import xyz.cloudkeeper.model.api.Marshaler;
import xyz.cloudkeeper.model.api.UnmarshalContext;
import xyz.cloudkeeper.model.beans.element.serialization.MutableSerializationDeclaration;

import java.io.IOException;
import java.util.SortedMap;

public final class SortedMapMarshaler implements Marshaler<SortedMap<?, ?>> {
    @Override
    public boolean isImmutable(SortedMap<?, ?> object) {
        return false;
    }

    @Override
    public void put(SortedMap<?, ?> object, MarshalContext context) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SortedMap<?, ?> get(UnmarshalContext context) throws IOException {
        throw new UnsupportedOperationException();
    }


    public static final class Beans {
        private Beans() { }

        public static MutableSerializationDeclaration declaration() {
            return MutableSerializationDeclaration.fromClass(SortedMapMarshaler.class);
        }
    }
}
