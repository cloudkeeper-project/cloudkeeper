package xyz.cloudkeeper.marshaling;

import xyz.cloudkeeper.model.api.MarshalContext;
import xyz.cloudkeeper.model.api.Marshaler;
import xyz.cloudkeeper.model.api.UnmarshalContext;
import xyz.cloudkeeper.model.immutable.element.NoKey;

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
