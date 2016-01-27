package com.svbio.cloudkeeper.marshaling;

import com.svbio.cloudkeeper.model.api.MarshalContext;
import com.svbio.cloudkeeper.model.api.Marshaler;
import com.svbio.cloudkeeper.model.api.MarshalingException;
import com.svbio.cloudkeeper.model.immutable.element.NoKey;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DelegatingMarshalContextTest {
    private static final IntegerMarshaler INTEGER_MARSHALER = new IntegerMarshaler();
    private static final StringMarshaler STRING_MARSHALER = new StringMarshaler();

    @Test
    public void findMarshalerFound() throws MarshalingException {
        Assert.assertSame(
            DelegatingMarshalContext.findMarshaler(
                4,
                Arrays.asList(STRING_MARSHALER, INTEGER_MARSHALER)
            ),
            INTEGER_MARSHALER
        );

        Assert.assertSame(
            DelegatingMarshalContext.findMarshaler(
                4,
                Arrays.asList(INTEGER_MARSHALER, STRING_MARSHALER)
            ),
            INTEGER_MARSHALER
        );
    }

    @Test
    public void findMarshalerNotFound() {
        try {
            DelegatingMarshalContext.findMarshaler(
                4.0,
                Arrays.asList(INTEGER_MARSHALER, STRING_MARSHALER)
            );
            Assert.fail();
        } catch (MarshalingException exception) {
            Assert.assertTrue(exception.getMessage().startsWith("None of"));
            Assert.assertTrue(exception.getMessage().contains("is capable of marshaling instance:"));
            // Expected!
        }
    }

    @Test
    public void findMarshalerBadArguments() throws MarshalingException {
        try {
            DelegatingMarshalContext.findMarshaler(
                null,
                Arrays.asList(INTEGER_MARSHALER, STRING_MARSHALER)
            );
            Assert.fail();
        } catch (NullPointerException ignored) { }

        try {
            DelegatingMarshalContext.findMarshaler(4.0, null);
            Assert.fail();
        } catch (NullPointerException ignored) { }
    }

    @Test
    public void create() throws IOException {
        MarshalingTreeBuilder treeBuilder = MarshalingTreeBuilder.create(
            (path, marshaler, object) -> !path.equals(Collections.singletonList(NoKey.instance()))
        );
        TreeBuilderMarshalTarget<MarshalingTreeNode> marshalTarget = TreeBuilderMarshalTarget.create(treeBuilder);
        List<Marshaler<?>> marshalers = Arrays.asList(STRING_MARSHALER, INTEGER_MARSHALER);
        try (MarshalContext marshalContext
                = DelegatingMarshalContext.create(INTEGER_MARSHALER, marshalers, marshalTarget)) {
            INTEGER_MARSHALER.put(42, marshalContext);
        }
        Assert.assertEquals(
            marshalTarget.getTree(),
            MarshalingTreeNode.MarshaledReplacementObjectNode.of(
                INTEGER_MARSHALER,
                MarshalingTreeNode.RawObjectNode.of(STRING_MARSHALER, "42")
            )
        );
    }
}
