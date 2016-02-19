package xyz.cloudkeeper.model.beans.element.serialization;

import xyz.cloudkeeper.model.bare.element.serialization.BareByteSequence;
import xyz.cloudkeeper.model.bare.element.serialization.BareSerializationNodeVisitor;
import xyz.cloudkeeper.model.bare.element.serialization.BareSerializationRoot;
import xyz.cloudkeeper.model.bare.element.serialization.BareSerializedString;
import xyz.cloudkeeper.model.beans.element.MutableQualifiedNamable;
import xyz.cloudkeeper.model.immutable.element.Key;
import xyz.cloudkeeper.model.immutable.element.NoKey;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.List;

final class JAXBAdapters {
    private JAXBAdapters() {
        throw new AssertionError(String.format("No %s instances for you!", getClass().getName()));
    }

    private interface JAXBSerializationNode {
        MutableSerializationNode<?> toMutableSerializationNode();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    static final class JAXBByteSequence implements JAXBSerializationNode {
        @XmlAttribute
        @Nullable
        private Key id;

        @XmlValue
        @Nullable
        private byte[] byteSequence;

        @Override
        public MutableByteSequence toMutableSerializationNode() {
            return new MutableByteSequence()
                .setKey(id)
                .setArray(byteSequence);
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    static final class JAXBSerializedString implements JAXBSerializationNode {
        @XmlAttribute
        @Nullable
        private Key id;

        @XmlValue
        @Nullable
        private String string;

        @Override
        public MutableSerializedString toMutableSerializationNode() {
            return new MutableSerializedString()
                .setKey(id)
                .setString(string);
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    static final class JAXBSerializationRoot implements JAXBSerializationNode {
        @XmlAttribute
        @Nullable
        private Key id;

        @XmlAttribute
        @Nullable
        private MutableQualifiedNamable ref;

        @XmlElements({
            @XmlElement(type = JAXBByteSequence.class, name = "serialized-as-byte-stream"),
            @XmlElement(type = JAXBSerializedString.class, name = "serialized-as-string"),
            @XmlElement(type = JAXBSerializationRoot.class, name = "serialized-as-tree")
        })
        @Nullable
        private List<MutableSerializationNode<?>> entries;

        @Override
        public MutableSerializationRoot toMutableSerializationNode() {
            MutableSerializationRoot serializationRoot = new MutableSerializationRoot()
                .setKey(
                    id == null
                        ? NoKey.instance()
                        : id
                )
                .setDeclaration(ref);
            if (entries != null) {
                serializationRoot.setEntries(entries);
            }
            return serializationRoot;
        }
    }

    enum ToJAXBVisitor implements BareSerializationNodeVisitor<Object, Void> {
        INSTANCE;

        @Override
        public JAXBSerializationRoot visitRoot(BareSerializationRoot node, @Nullable Void parameter) {
            MutableSerializationRoot mutableNode = (MutableSerializationRoot) node;
            JAXBSerializationRoot jaxbSerializationRoot = new JAXBSerializationRoot();
            jaxbSerializationRoot.id = mutableNode.getKey();
            jaxbSerializationRoot.ref = mutableNode.getDeclaration();
            jaxbSerializationRoot.entries = mutableNode.getEntries();
            return jaxbSerializationRoot;
        }

        @Override
        public JAXBByteSequence visitByteSequence(BareByteSequence node, @Nullable Void parameter) {
            JAXBByteSequence jaxbByteSequence = new JAXBByteSequence();
            jaxbByteSequence.id = node.getKey();
            jaxbByteSequence.byteSequence = node.getArray();
            return jaxbByteSequence;
        }

        @Override
        public JAXBSerializedString visitText(BareSerializedString node, @Nullable Void parameter) {
            JAXBSerializedString jaxbSerializedString = new JAXBSerializedString();
            jaxbSerializedString.id = node.getKey();
            jaxbSerializedString.string = node.getString();
            return jaxbSerializedString;
        }
    }

    static final class SerializationNodeAdapter extends XmlAdapter<Object, MutableSerializationNode<?>> {
        @Override
        @Nullable
        public MutableSerializationNode<?> unmarshal(@Nullable Object original) {
            return original == null
                ? null
                : ((JAXBSerializationNode) original).toMutableSerializationNode();
        }

        @Override
        @Nullable
        public Object marshal(@Nullable MutableSerializationNode<?> original) {
            return original == null
                ? null
                : original.accept(ToJAXBVisitor.INSTANCE, null);
        }
    }

    static final class SerializationRootAdapter extends XmlAdapter<JAXBSerializationRoot, MutableSerializationRoot> {
        @Override
        @Nullable
        public MutableSerializationRoot unmarshal(@Nullable JAXBSerializationRoot original) {
            return original == null
                ? null
                : original.toMutableSerializationNode();
        }

        @Override
        @Nullable
        public JAXBSerializationRoot marshal(@Nullable MutableSerializationRoot original) {
            return original == null
                ? null
                : ToJAXBVisitor.INSTANCE.visitRoot(original, null);
        }
    }
}
