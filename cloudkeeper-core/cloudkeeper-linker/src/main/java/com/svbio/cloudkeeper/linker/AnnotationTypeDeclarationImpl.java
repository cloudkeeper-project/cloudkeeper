package com.svbio.cloudkeeper.linker;

import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.bare.element.BarePluginDeclarationVisitor;
import com.svbio.cloudkeeper.model.bare.element.annotation.BareAnnotationTypeDeclaration;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;
import com.svbio.cloudkeeper.model.runtime.element.RuntimeElement;
import com.svbio.cloudkeeper.model.runtime.element.RuntimePluginDeclarationVisitor;
import com.svbio.cloudkeeper.model.runtime.element.annotation.RuntimeAnnotationTypeDeclaration;
import com.svbio.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

final class AnnotationTypeDeclarationImpl extends PluginDeclarationImpl implements RuntimeAnnotationTypeDeclaration {
    private final Map<SimpleName, AnnotationTypeElementImpl> elementMap;

    AnnotationTypeDeclarationImpl(BareAnnotationTypeDeclaration original, CopyContext parentContext)
            throws LinkerException {
        super(original, parentContext);
        elementMap = unmodifiableMapOf(original.getElements(), "elements", AnnotationTypeElementImpl::new,
            AnnotationTypeElementImpl::getSimpleName);
    }

    @Override
    @Nullable
    public <T, P> T accept(BarePluginDeclarationVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visit(this, parameter);
    }

    @Override
    @Nullable
    public <T, P> T accept(RuntimePluginDeclarationVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visit(this, parameter);
    }

    @Override
    public String toString() {
        return BareAnnotationTypeDeclaration.Default.toString(this);
    }

    @Override
    public ImmutableList<? extends IElementImpl> getEnclosedElements() {
        return ImmutableList.copyOf(elementMap.values());
    }

    @Override
    public <T extends RuntimeElement> T getEnclosedElement(Class<T> clazz, SimpleName simpleName) {
        AnnotationTypeElementImpl element = elementMap.get(simpleName);
        if (clazz.isInstance(element)) {
            @SuppressWarnings("unchecked")
            T typedElement = (T) element;
            return typedElement;
        } else {
            return null;
        }
    }

    @Override
    public IElementImpl getEnclosingElement() {
        return null;
    }

    @Override
    public PluginDeclarationImpl getSuperAnnotatedConstruct() {
        return null;
    }

    @Override
    public ImmutableList<AnnotationTypeElementImpl> getElements() {
        return ImmutableList.copyOf(elementMap.values());
    }

    @Override
    void collectEnclosedByAnnotatedConstruct(Collection<AbstractFreezable> freezables) {
        freezables.addAll(elementMap.values());
    }

    @Override
    void finishFreezable(FinishContext context) { }

    @Override
    void verifyFreezable(VerifyContext context) { }
}
