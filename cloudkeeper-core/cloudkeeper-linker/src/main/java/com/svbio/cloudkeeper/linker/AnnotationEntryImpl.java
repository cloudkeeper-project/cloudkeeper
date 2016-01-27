package com.svbio.cloudkeeper.linker;

import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.bare.element.annotation.BareAnnotationEntry;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;
import com.svbio.cloudkeeper.model.runtime.element.annotation.RuntimeAnnotationEntry;
import com.svbio.cloudkeeper.model.runtime.element.annotation.RuntimeAnnotationTypeElement;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

final class AnnotationEntryImpl
        extends LocatableImpl
        implements RuntimeAnnotationEntry, Map.Entry<RuntimeAnnotationTypeElement, AnnotationValueImpl> {
    private final SimpleNameReference keyReference;
    private final AnnotationValueImpl value;
    @Nullable private volatile AnnotationTypeElementImpl key;

    AnnotationEntryImpl(BareAnnotationEntry original, CopyContext parentContext) throws LinkerException {
        super(original, parentContext);
        CopyContext context = getCopyContext();
        keyReference = new SimpleNameReference(original.getKey(), context.newContextForProperty("key"));
        value = new AnnotationValueImpl(original.getValue(), context.newContextForProperty("value"));
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        require(State.FINISHED);
        @Nullable AnnotationTypeElementImpl localKey = key;
        assert localKey != null : "must be non-null when finished";

        AnnotationEntryImpl other = (AnnotationEntryImpl) otherObject;
        return localKey.equals(other.key) && value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return 127 * keyReference.getSimpleName().toString().hashCode() ^ value.hashCode();
    }

    @Override
    public String toString() {
        return Default.toString(this);
    }

    SimpleName getKeyName() {
        return keyReference.getSimpleName();
    }

    @Override
    public AnnotationTypeElementImpl getKey() {
        require(State.FINISHED);
        @Nullable AnnotationTypeElementImpl localKey = key;
        assert localKey != null : "must be non-null when finished";
        return localKey;
    }

    @Override
    public AnnotationValueImpl getValue() {
        return value;
    }


    /**
     * Since this class is immutable, calling this method throws an {@link UnsupportedOperationException}.
     *
     * @throws UnsupportedOperationException whenever this method is called
     */
    @Deprecated
    @Override
    public AnnotationValueImpl setValue(@Nullable AnnotationValueImpl value) {
        throw new UnsupportedOperationException("setValue() called on immutable object.");
    }

    @Override
    void collectEnclosed(Collection<AbstractFreezable> freezables) {
        freezables.add(keyReference);
        freezables.add(value);
    }

    @Override
    void preProcessFreezable(FinishContext context) { }

    @Override
    final void finishFreezable(FinishContext context) throws LinkerException {
        key = context.getAnnotationTypeElement(keyReference);
    }

    @Override
    void verifyFreezable(VerifyContext context) { }
}
