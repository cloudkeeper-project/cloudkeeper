package com.svbio.cloudkeeper.marshaling;

import com.svbio.cloudkeeper.model.api.MarshalContext;
import com.svbio.cloudkeeper.model.api.Marshaler;
import com.svbio.cloudkeeper.model.api.UnmarshalContext;
import com.svbio.cloudkeeper.model.immutable.element.NoKey;

import java.io.IOException;

final class IntegerMarshaler implements Marshaler<Integer> {
    @Override
    public boolean isImmutable(Integer object) {
        return true;
    }

    @Override
    public void put(Integer object, MarshalContext context) throws IOException {
        context.writeObject(object.toString(), NoKey.instance());
    }

    @Override
    public Integer get(UnmarshalContext context) throws IOException {
        return Integer.valueOf((String) context.readObject(NoKey.instance()));
    }
}
