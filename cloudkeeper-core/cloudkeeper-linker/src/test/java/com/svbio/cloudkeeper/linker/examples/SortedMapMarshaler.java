package com.svbio.cloudkeeper.linker.examples;

import com.svbio.cloudkeeper.model.api.MarshalContext;
import com.svbio.cloudkeeper.model.api.Marshaler;
import com.svbio.cloudkeeper.model.api.UnmarshalContext;
import com.svbio.cloudkeeper.model.beans.element.serialization.MutableSerializationDeclaration;

import java.io.IOException;
import java.util.SortedMap;

public final class SortedMapMarshaler implements Marshaler<SortedMap<?,?>> {
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


    public static class Beans {
        private Beans() { }

        public static MutableSerializationDeclaration declaration() {
            return MutableSerializationDeclaration.fromClass(SortedMapMarshaler.class);
        }
    }
}
