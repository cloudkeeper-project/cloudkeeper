package com.svbio.cloudkeeper.marshaling;

import com.svbio.cloudkeeper.marshaling.MarshalingTreeNode.MarshaledReplacementObjectNode;
import com.svbio.cloudkeeper.marshaling.MarshalingTreeNode.ObjectNode;
import com.svbio.cloudkeeper.marshaling.MarshalingTreeNode.RawObjectNode;
import com.svbio.cloudkeeper.model.api.UnmarshalContext;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;

public class DelegatingUnmarshalContextTest {
    private static final IntegerMarshaler INTEGER_MARSHALER = new IntegerMarshaler();
    private static final StringMarshaler STRING_MARSHALER = new StringMarshaler();

    @Test
    public void create() throws IOException {
        ObjectNode tree
            = MarshaledReplacementObjectNode.of(INTEGER_MARSHALER, RawObjectNode.of(STRING_MARSHALER, "42"));
        UnmarshalContext unmarshalContext = DelegatingUnmarshalContext.create(
            MarshalingTreeUnmarshalSource.create(tree),
            getClass().getClassLoader()
        );
        Assert.assertEquals(INTEGER_MARSHALER.get(unmarshalContext), (Integer) 42);
    }
}
