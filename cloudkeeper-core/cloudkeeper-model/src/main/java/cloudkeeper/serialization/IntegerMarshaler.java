package cloudkeeper.serialization;

import xyz.cloudkeeper.model.CloudKeeperSerialization;
import xyz.cloudkeeper.model.api.MarshalContext;
import xyz.cloudkeeper.model.api.Marshaler;
import xyz.cloudkeeper.model.api.UnmarshalContext;
import xyz.cloudkeeper.model.immutable.element.NoKey;

import java.io.IOException;

/**
 * CloudKeeper Integer serialization that uses the {@link Integer#toString()} representation.
 *
 * <p>Implementation note: This serialization class does not write out any explicit type information -- instead, it
 * relies on the fact that serialization contexts need to preserve which {@link Marshaler} class was used for
 * serialization. This is enough because {@link Integer} is a final class.
 */
@CloudKeeperSerialization(StringMarshaler.class)
public final class IntegerMarshaler implements Marshaler<Integer> {
    @Override
    public boolean isImmutable(Integer object) {
        return true;
    }

    @Override
    public void put(Integer value, MarshalContext context) throws IOException {
        context.writeObject(value.toString(), NoKey.instance());
    }

    @Override
    public Integer get(UnmarshalContext context) throws IOException {
        return Integer.valueOf((String) context.readObject(NoKey.instance()));
    }
}
