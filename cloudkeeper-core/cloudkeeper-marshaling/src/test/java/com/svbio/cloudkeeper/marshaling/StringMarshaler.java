package com.svbio.cloudkeeper.marshaling;

import com.svbio.cloudkeeper.model.api.MarshalContext;
import com.svbio.cloudkeeper.model.api.Marshaler;
import com.svbio.cloudkeeper.model.api.UnmarshalContext;
import com.svbio.cloudkeeper.model.immutable.element.NoKey;
import com.svbio.cloudkeeper.model.util.ByteSequences;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

final class StringMarshaler implements Marshaler<String> {
    @Override
    public boolean isImmutable(String string) {
        return true;
    }

    @Override
    public void put(String string, MarshalContext context) throws IOException {
        context.putByteSequence(
            ByteSequences.arrayBacked(string.getBytes(StandardCharsets.UTF_8)),
            NoKey.instance()
        );
    }

    @Override
    public String get(UnmarshalContext context) throws IOException {
        return new String(
            context.getByteSequence(NoKey.instance()).toByteArray(),
            StandardCharsets.UTF_8
        );
    }
}
