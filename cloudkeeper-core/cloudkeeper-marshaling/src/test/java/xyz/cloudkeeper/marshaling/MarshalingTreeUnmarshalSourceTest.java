package xyz.cloudkeeper.marshaling;

import org.testng.Assert;
import org.testng.annotations.Test;
import xyz.cloudkeeper.marshaling.MarshalingTreeNode.ObjectNode;
import xyz.cloudkeeper.model.api.Marshaler;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class MarshalingTreeUnmarshalSourceTest {
    private static final IntegerMarshaler INTEGER_MARSHALER = new IntegerMarshaler();
    private static final StringMarshaler STRING_MARSHALER = new StringMarshaler();
    private static final PersonMarshaler PERSON_MARSHALER = new PersonMarshaler();

    @Test
    public void unmarshal() throws IOException {
        Person person = new Person(42, "Alice");
        List<Marshaler<?>> marshalers = Arrays.asList(INTEGER_MARSHALER, STRING_MARSHALER, PERSON_MARSHALER);
        ObjectNode tree = MarshalingTreeBuilder.marshal(person, marshalers, (path, marshaler, object) -> true);
        Assert.assertEquals(MarshalingTreeUnmarshalSource.unmarshal(tree, getClass().getClassLoader()), person);
    }
}
