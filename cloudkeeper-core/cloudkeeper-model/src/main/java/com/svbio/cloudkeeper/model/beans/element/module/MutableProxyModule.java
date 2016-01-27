package com.svbio.cloudkeeper.model.beans.element.module;

import com.svbio.cloudkeeper.model.bare.element.module.BareModuleVisitor;
import com.svbio.cloudkeeper.model.bare.element.module.BareProxyModule;
import com.svbio.cloudkeeper.model.beans.CopyOption;
import com.svbio.cloudkeeper.model.beans.element.MutableQualifiedNamable;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

@XmlRootElement(name = "proxy-module")
public final class MutableProxyModule extends MutableModule<MutableProxyModule> implements BareProxyModule {
    private static final long serialVersionUID = 5394012602906125856L;

    @Nullable private MutableQualifiedNamable declaration;

    public MutableProxyModule() { }

    private MutableProxyModule(BareProxyModule original, CopyOption[] copyOptions) {
        super(original, copyOptions);
        declaration = MutableQualifiedNamable.copyOf(original.getDeclaration(), copyOptions);
    }

    @Nullable
    public static MutableProxyModule copyOfProxyModule(@Nullable BareProxyModule original, CopyOption... copyOptions) {
        return original == null
            ? null
            : new MutableProxyModule(original, copyOptions);
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (!super.equals(otherObject)) {
            return false;
        }
        return Objects.equals(declaration, ((MutableProxyModule) otherObject).declaration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(declaration);
    }

    @Override
    public String toString() {
        return BareProxyModule.Default.toString(this);
    }

    @Override
    protected MutableProxyModule self() {
        return this;
    }

    @Override
    @Nullable
    public <T, P> T accept(BareModuleVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitLinkedModule(this, parameter);
    }

    @XmlAttribute(name = "ref")
    @Override
    @Nullable
    public MutableQualifiedNamable getDeclaration() {
        return declaration;
    }

    public MutableProxyModule setDeclaration(@Nullable MutableQualifiedNamable declaration) {
        this.declaration = declaration;
        return this;
    }

    public MutableProxyModule setDeclaration(String declarationName) {
        return setDeclaration(
            new MutableQualifiedNamable().setQualifiedName(declarationName)
        );
    }
}
