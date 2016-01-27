package com.svbio.cloudkeeper.model.beans.element.module;

import com.svbio.cloudkeeper.model.bare.element.BarePluginDeclarationVisitor;
import com.svbio.cloudkeeper.model.bare.element.module.BareCompositeModuleDeclaration;
import com.svbio.cloudkeeper.model.bare.element.module.BareModuleDeclaration;
import com.svbio.cloudkeeper.model.bare.element.module.BareModuleDeclarationVisitor;
import com.svbio.cloudkeeper.model.bare.element.module.BareSimpleModuleDeclaration;
import com.svbio.cloudkeeper.model.beans.CopyOption;
import com.svbio.cloudkeeper.model.beans.element.MutablePluginDeclaration;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlSeeAlso;

@XmlSeeAlso({ MutableCompositeModuleDeclaration.class, MutableSimpleModuleDeclaration.class })
public abstract class MutableModuleDeclaration<D extends MutableModuleDeclaration<D>>
        extends MutablePluginDeclaration<D>
        implements BareModuleDeclaration {
    private static final long serialVersionUID = -3405633500476304133L;

    MutableModuleDeclaration() { }

    MutableModuleDeclaration(BareModuleDeclaration original, CopyOption[] copyOptions) {
        super(original, copyOptions);
    }

    private enum CopyVisitor implements BareModuleDeclarationVisitor<MutableModuleDeclaration<?>, CopyOption[]> {
        INSTANCE;

        @Override
        public MutableModuleDeclaration<?> visit(BareCompositeModuleDeclaration declaration,
                @Nullable CopyOption[] copyOptions) {
            assert copyOptions != null;
            return MutableCompositeModuleDeclaration.copyOfCompositeModuleDeclaration(declaration, copyOptions);
        }

        @Override
        public MutableModuleDeclaration<?> visit(BareSimpleModuleDeclaration declaration,
                @Nullable CopyOption[] copyOptions) {
            assert copyOptions != null;
            return MutableSimpleModuleDeclaration.copyOfSimpleModuleDeclaration(declaration, copyOptions);
        }
    }

    @Nullable
    public static MutableModuleDeclaration<?> copyOfModuleDeclaration(@Nullable BareModuleDeclaration original,
            CopyOption... copyOptions) {
        return original != null
            ? original.accept(CopyVisitor.INSTANCE, copyOptions)
            : null;
    }

    @Override
    @Nullable
    public final <T, P> T accept(BarePluginDeclarationVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visit(this, parameter);
    }
}
