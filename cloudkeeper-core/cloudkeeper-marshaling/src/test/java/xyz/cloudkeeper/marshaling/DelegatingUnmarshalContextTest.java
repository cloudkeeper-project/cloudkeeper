package xyz.cloudkeeper.marshaling;

import org.testng.Assert;
import org.testng.annotations.Test;
import xyz.cloudkeeper.marshaling.MarshalingTreeNode.MarshaledReplacementObjectNode;
import xyz.cloudkeeper.marshaling.MarshalingTreeNode.ObjectNode;
import xyz.cloudkeeper.marshaling.MarshalingTreeNode.RawObjectNode;
import xyz.cloudkeeper.model.api.UnmarshalContext;

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
