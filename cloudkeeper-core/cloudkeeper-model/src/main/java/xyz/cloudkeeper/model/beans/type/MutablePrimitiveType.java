package xyz.cloudkeeper.model.beans.type;

import xyz.cloudkeeper.model.bare.type.BarePrimitiveType;
import xyz.cloudkeeper.model.bare.type.BareTypeMirrorVisitor;
import xyz.cloudkeeper.model.beans.CopyOption;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@XmlRootElement(name = "primitive-type")
public final class MutablePrimitiveType extends MutableTypeMirror<MutablePrimitiveType> implements BarePrimitiveType {
    private static final long serialVersionUID = 7572100998849035229L;

    @Nullable private Kind kind;

    public MutablePrimitiveType() { }

    private static final Map<Class<?>, Kind> JAVA_CLASS_MAP;
    static {
        Map<Class<?>, Kind> newMap = new HashMap<>();
        for (Kind primitiveType: Kind.values()) {
            newMap.put(primitiveType.getPrimitiveClass(), primitiveType);
        }
        JAVA_CLASS_MAP = Collections.unmodifiableMap(newMap);
    }

    /**
     * Copy constructor with class object.
     *
     * @throws IllegalArgumentException if the given class does not represent a primitive type, that is, if
     *     {@link Class#isPrimitive()} returns false
     */
    private MutablePrimitiveType(Class<?> clazz, CopyOption[] copyOptions) {
        if (!clazz.isPrimitive()) {
            throw new IllegalArgumentException(String.format(
                "Expected Class object representing primitive type, but got %s.", clazz
            ));
        }
        kind = JAVA_CLASS_MAP.get(clazz);
    }

    public static MutablePrimitiveType fromClass(Class<?> clazz, CopyOption... copyOptions) {
        return new MutablePrimitiveType(clazz, copyOptions);
    }

    private MutablePrimitiveType(BarePrimitiveType original, CopyOption[] copyOptions) {
        super(original, copyOptions);
        kind = original.getPrimitiveKind();
    }

    @Nullable
    public static MutablePrimitiveType copyOfPrimitiveType(@Nullable BarePrimitiveType original,
            CopyOption... copyOptions) {
        return original == null
            ? null
            : new MutablePrimitiveType(original, copyOptions);
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (!super.equals(otherObject)) {
            return false;
        }
        return Objects.equals(kind, ((MutablePrimitiveType) otherObject).kind);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(kind);
    }

    @Override
    public String toString() {
        return BarePrimitiveType.Default.toString(this);
    }

    @Override
    @Nullable
    public <T, P> T accept(BareTypeMirrorVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitPrimitive(this, parameter);
    }

    @Override
    protected MutablePrimitiveType self() {
        return this;
    }

    @XmlAttribute(name = "ref")
    @Override
    @Nullable
    public Kind getPrimitiveKind() {
        return kind;
    }

    /**
     * Sets the concrete primitive type represented by this object.
     */
    public MutablePrimitiveType setPrimitiveKind(@Nullable Kind kind) {
        this.kind = kind;
        return this;
    }
}
