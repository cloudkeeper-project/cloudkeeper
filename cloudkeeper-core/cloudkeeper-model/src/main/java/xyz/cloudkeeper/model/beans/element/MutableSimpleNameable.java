package xyz.cloudkeeper.model.beans.element;

import xyz.cloudkeeper.model.bare.element.BareSimpleNameable;
import xyz.cloudkeeper.model.beans.CopyOption;
import xyz.cloudkeeper.model.beans.MutableLocatable;
import xyz.cloudkeeper.model.immutable.element.SimpleName;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Objects;

@XmlJavaTypeAdapter(JAXBAdapters.MutableSimpleNamableAdapter.class)
public final class MutableSimpleNameable extends MutableLocatable<MutableSimpleNameable> implements BareSimpleNameable {
    private static final long serialVersionUID = -3265430667585446759L;

    @Nullable private SimpleName simpleName;

    public MutableSimpleNameable() { }

    private MutableSimpleNameable(BareSimpleNameable original, CopyOption[] copyOptions) {
        super(original, copyOptions);
        simpleName = original.getSimpleName();
    }

    /**
     * Returns a copy of the given instance, or {@code null} if the given instance is {@code null}.
     */
    @Nullable
    public static MutableSimpleNameable copyOf(@Nullable BareSimpleNameable original, CopyOption... copyOptions) {
        return original != null
            ? new MutableSimpleNameable(original, copyOptions)
            : null;
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        return Objects.equals(simpleName, ((MutableSimpleNameable) otherObject).simpleName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(simpleName);
    }

    @Override
    public String toString() {
        return Default.toString(this);
    }

    @Override
    protected MutableSimpleNameable self() {
        return this;
    }

    @Override
    @Nullable
    public SimpleName getSimpleName() {
        return simpleName;
    }

    public MutableSimpleNameable setSimpleName(@Nullable SimpleName simpleName) {
        this.simpleName = simpleName;
        return this;
    }

    public MutableSimpleNameable setSimpleName(String simpleName) {
        return setSimpleName(SimpleName.identifier(simpleName));
    }
}
