package xyz.cloudkeeper.marshaling;

import org.testng.Assert;
import org.testng.annotations.Test;
import xyz.cloudkeeper.marshaling.MarshalingTreeNode.ByteSequenceNode;
import xyz.cloudkeeper.marshaling.MarshalingTreeNode.MarshaledReplacementObjectNode;
import xyz.cloudkeeper.marshaling.MarshalingTreeNode.ObjectNode;
import xyz.cloudkeeper.marshaling.MarshalingTreeNode.RawObjectNode;
import xyz.cloudkeeper.model.api.Marshaler;
import xyz.cloudkeeper.model.immutable.element.Key;
import xyz.cloudkeeper.model.immutable.element.SimpleName;
import xyz.cloudkeeper.model.util.ByteSequences;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MarshalingTreeBuilderTest {
    private static final IntegerMarshaler INTEGER_MARSHALER = new IntegerMarshaler();
    private static final StringMarshaler STRING_MARSHALER = new StringMarshaler();
    private static final PersonMarshaler PERSON_MARSHALER = new PersonMarshaler();

    @Test
    public void marshalInteger() throws IOException {
        List<Marshaler<?>> marshalers = Arrays.asList(STRING_MARSHALER, INTEGER_MARSHALER);
        ObjectNode actual = MarshalingTreeBuilder.marshal(42, marshalers, (path, marshaler, object) -> true);
        Assert.assertEquals(
            actual,
            MarshaledReplacementObjectNode.of(
                INTEGER_MARSHALER,
                MarshaledReplacementObjectNode.of(
                    STRING_MARSHALER,
                    ByteSequenceNode.of(ByteSequences.arrayBacked("42".getBytes(StandardCharsets.UTF_8)))
                )
            )
        );
    }

    @Test
    public void processMarshalingTreeInteger() throws IOException {
        ObjectNode source = MarshaledReplacementObjectNode.of(
            INTEGER_MARSHALER,
            RawObjectNode.of(STRING_MARSHALER, "42")
        );

        List<Marshaler<?>> marshalers = Arrays.asList(STRING_MARSHALER, INTEGER_MARSHALER);
        ObjectNode actual
            = MarshalingTreeBuilder.processMarshalingTree(source, marshalers, (path, marshaler, object) -> true);
        Assert.assertEquals(
            actual,
            MarshaledReplacementObjectNode.of(
                INTEGER_MARSHALER,
                MarshaledReplacementObjectNode.of(
                    STRING_MARSHALER,
                    ByteSequenceNode.of(ByteSequences.arrayBacked("42".getBytes(StandardCharsets.UTF_8)))
                )
            )
        );

        // Verify that marshaling is idempotent (if parameters are the same)
        Assert.assertEquals(
            MarshalingTreeBuilder.processMarshalingTree(actual, marshalers, (path, marshaler, object) -> true),
            MarshaledReplacementObjectNode.of(
                INTEGER_MARSHALER,
                MarshaledReplacementObjectNode.of(
                    STRING_MARSHALER,
                    ByteSequenceNode.of(ByteSequences.arrayBacked("42".getBytes(StandardCharsets.UTF_8)))
                )
            )
        );
    }

    private static final class MapBuilder<K, V> {
        private final Map<K, V> map;

        private MapBuilder(Map<K, V> map) {
            this.map = map;
        }

        private MapBuilder<K, V> put(K key, V value) {
            map.put(key, value);
            return this;
        }

        private Map<K, V> toMap() {
            return map;
        }
    }

    @Test
    public void marshalPerson() throws IOException {
        Person person = new Person(42, "Alice");
        List<Marshaler<?>> marshalers = Arrays.asList(INTEGER_MARSHALER, STRING_MARSHALER, PERSON_MARSHALER);
        ObjectNode actual = MarshalingTreeBuilder.marshal(person, marshalers, (path, marshaler, object) -> true);
        Assert.assertEquals(
            actual,
            MarshalingTreeNode.MarshaledObjectNode.of(
                PERSON_MARSHALER,
                new MapBuilder<Key, MarshalingTreeNode>(new LinkedHashMap<>())
                    .put(
                        SimpleName.identifier("age"),
                        MarshaledReplacementObjectNode.of(
                            INTEGER_MARSHALER,
                            MarshaledReplacementObjectNode.of(
                                STRING_MARSHALER,
                                ByteSequenceNode.of(ByteSequences.arrayBacked("42".getBytes(StandardCharsets.UTF_8)))
                            )
                        )
                    )
                    .put(
                        SimpleName.identifier("name"),
                        ByteSequenceNode.of(ByteSequences.arrayBacked("Alice".getBytes(StandardCharsets.UTF_8)))
                    )
                    .toMap()
            )
        );
        Assert.assertEquals(MarshalingTreeUnmarshalSource.unmarshal(actual, getClass().getClassLoader()), person);
    }
}
