package com.svbio.cloudkeeper.marshaling;

import cloudkeeper.types.ByteSequence;
import com.svbio.cloudkeeper.model.api.Marshaler;
import com.svbio.cloudkeeper.model.immutable.element.Key;
import com.svbio.cloudkeeper.model.immutable.element.NoKey;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

abstract class MarshalingTreeNodeImpl implements MarshalingTreeNode {
    @Override
    public abstract boolean equals(Object otherObject);

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();

    static final class ByteSequenceNodeImpl extends MarshalingTreeNodeImpl implements ByteSequenceNode {
        private static final int MAX_DISPLAY_BYTES = 16;
        private final ByteSequence byteSequence;

        ByteSequenceNodeImpl(ByteSequence byteSequence) {
            Objects.requireNonNull(byteSequence);
            this.byteSequence = byteSequence;
        }

        private byte[] bytes() {
            try {
                return byteSequence.toByteArray();
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }

        @Override
        public boolean equals(@Nullable Object otherObject) {
            return this == otherObject || (
                otherObject != null
                    && getClass() == otherObject.getClass()
                    && Arrays.equals(bytes(), ((ByteSequenceNodeImpl) otherObject).bytes())
            );
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(bytes());
        }

        @Override
        public String toString() {
            byte[] localBytes = bytes();
            String content = localBytes.length > MAX_DISPLAY_BYTES
                ? Base64.getEncoder().encodeToString(Arrays.copyOf(localBytes, MAX_DISPLAY_BYTES)) + "..."
                : Base64.getEncoder().encodeToString(localBytes);
            return String.format("byte-sequence node (content %s)", content);
        }

        @Override
        public ByteSequence getByteSequence() {
            return byteSequence;
        }
    }

    abstract static class ObjectNodeImpl extends MarshalingTreeNodeImpl implements ObjectNode {
        private final Marshaler<?> marshaler;

        private ObjectNodeImpl(Marshaler<?> marshaler) {
            Objects.requireNonNull(marshaler);
            this.marshaler = marshaler;
        }

        @Override
        public boolean equals(@Nullable Object otherObject) {
            return otherObject != null
                && getClass() == otherObject.getClass()
                && marshaler.equals(((ObjectNodeImpl) otherObject).marshaler);
        }

        @Override
        public int hashCode() {
            return marshaler.hashCode();
        }

        @Nonnull
        @Override
        public Marshaler<?> getMarshaler() {
            return marshaler;
        }
    }

    static final class RawObjectNodeImpl extends ObjectNodeImpl implements RawObjectNode {
        private final Object object;

        RawObjectNodeImpl(Marshaler<?> marshaler, Object object) {
            super(marshaler);
            Objects.requireNonNull(object);
            this.object = object;
        }

        @Override
        public boolean equals(@Nullable Object otherObject) {
            return this == otherObject || (
                super.equals(otherObject)
                    && object.equals(((RawObjectNodeImpl) otherObject).object)
            );
        }

        @Override
        public int hashCode() {
            return 31 * super.hashCode() + object.hashCode();
        }

        @Override
        public String toString() {
            return String.format("raw-object node (%s)", object);
        }

        @Override
        public Object getObject() {
            return object;
        }
    }

    static final class MarshaledAsObjectNodeImpl extends ObjectNodeImpl implements MarshaledObjectNode {
        private final Map<Key, MarshalingTreeNode> children;

        MarshaledAsObjectNodeImpl(Marshaler<?> marshaler, Map<Key, MarshalingTreeNode> children) {
            super(marshaler);
            Objects.requireNonNull(children);
            for (Map.Entry<Key, MarshalingTreeNode> entry: children.entrySet()) {
                Objects.requireNonNull(entry.getKey());
                if (entry.getKey() instanceof NoKey) {
                    throw new IllegalArgumentException("Children cannot have empty keys.");
                }
                Objects.requireNonNull(entry.getValue());
            }
            this.children = new LinkedHashMap<>(children);
        }

        @Override
        public boolean equals(@Nullable Object otherObject) {
            return this == otherObject || (
                super.equals(otherObject)
                && children.equals(((MarshaledAsObjectNodeImpl) otherObject).children)
            );
        }

        @Override
        public int hashCode() {
            return 31 * super.hashCode() + children.hashCode();
        }

        @Override
        public String toString() {
            return String.format(
                "marshaled-object node (%s, direct children: %s)", getMarshaler().getClass(), children.keySet()
            );
        }

        @Override
        public Map<Key, MarshalingTreeNode> getChildren() {
            return children;
        }
    }

    static final class MarshaledAsReplacementObjectNodeImpl extends ObjectNodeImpl
            implements MarshaledReplacementObjectNode {
        private final MarshalingTreeNode child;

        MarshaledAsReplacementObjectNodeImpl(Marshaler<?> marshaler, MarshalingTreeNode child) {
            super(marshaler);
            Objects.requireNonNull(child);
            this.child = child;
        }

        @Override
        public boolean equals(@Nullable Object otherObject) {
            return this == otherObject || (
                super.equals(otherObject)
                && child.equals(((MarshaledAsReplacementObjectNodeImpl) otherObject).child)
            );
        }

        @Override
        public int hashCode() {
            return 31 * super.hashCode() + child.hashCode();
        }

        @Override
        public String toString() {
            return String.format("marshaled-replacement-object node (%s)", getMarshaler().getClass());
        }

        @Override
        public MarshalingTreeNode getChild() {
            return child;
        }
    }
}
