package xyz.cloudkeeper.model.beans.element.module;

import xyz.cloudkeeper.model.bare.element.module.BareCompositeModuleDeclaration;
import xyz.cloudkeeper.model.bare.element.module.BareModuleDeclarationVisitor;
import xyz.cloudkeeper.model.beans.CopyOption;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

@XmlRootElement(name = "composite-module-declaration")
public final class MutableCompositeModuleDeclaration
        extends MutableModuleDeclaration<MutableCompositeModuleDeclaration>
        implements BareCompositeModuleDeclaration {
    private static final long serialVersionUID = -2930636903424679052L;

    @Nullable
    private MutableCompositeModule template;

    public MutableCompositeModuleDeclaration() { }

    private MutableCompositeModuleDeclaration(BareCompositeModuleDeclaration original, CopyOption[] copyOptions) {
        super(original, copyOptions);
        template = MutableCompositeModule.copyOfCompositeModule(original.getTemplate(), copyOptions);
    }

    @Nullable
    public static MutableCompositeModuleDeclaration copyOfCompositeModuleDeclaration(
            @Nullable BareCompositeModuleDeclaration original, CopyOption... copyOptions) {
        return original == null
            ? null
            : new MutableCompositeModuleDeclaration(original, copyOptions);
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (!super.equals(otherObject)) {
            return false;
        }
        return Objects.equals(template, ((MutableCompositeModuleDeclaration) otherObject).template);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(template);
    }

    @Override
    public String toString() {
        return BareCompositeModuleDeclaration.Default.toString(this);
    }

    @Override
    protected MutableCompositeModuleDeclaration self() {
        return this;
    }

    @Override
    @Nullable
    public <T, P> T accept(BareModuleDeclarationVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visit(this, parameter);
    }

    @XmlElement
    @Override
    @Nullable
    public MutableCompositeModule getTemplate() {
        return template;
    }

    public MutableCompositeModuleDeclaration setTemplate(@Nullable MutableCompositeModule template) {
        this.template = template;
        return this;
    }
}
