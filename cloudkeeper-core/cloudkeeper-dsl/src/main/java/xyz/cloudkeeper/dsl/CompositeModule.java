package xyz.cloudkeeper.dsl;

import xyz.cloudkeeper.model.bare.element.module.BareCompositeModule;
import xyz.cloudkeeper.model.bare.element.module.BareModule;
import xyz.cloudkeeper.model.bare.element.module.BareModuleVisitor;
import xyz.cloudkeeper.model.immutable.element.SimpleName;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.List;

public abstract class CompositeModule<D extends CompositeModule<D>>
    extends DSLParentModule<D>
    implements BareCompositeModule {

    @Nullable private final DSLProxyModule linkedModule;

    protected CompositeModule() {
        // A composite module that does not have CompositeModulePlugin annotation is an anonymous module.
        linkedModule
            = (getDSLParent() != null && getModuleClass().isAnnotationPresent(CompositeModulePlugin.class))
                ? new DSLProxyModule(this)
                : null;

        createPorts(new PortVisitor() {
            @Override
            public void visitPortClass(SimpleName name, Class<?> portClass, Type type,
                List<DSLAnnotation> annotations) {

                if (InPort.class.isAssignableFrom(portClass)) {
                    new InPort<>(name, type, annotations);
                } else if (OutPort.class.isAssignableFrom(portClass)) {
                    new OutPort<>(name, type, annotations);
                }
            }
        });
    }

    @Override
    public final <T, P> T accept(BareModuleVisitor<T, P> visitor, P parameter) {
        return visitor.visitCompositeModule(this, parameter);
    }

    @Override
    public final String toString() {
        return BareCompositeModule.Default.toString(this);
    }

    @Override
    final BareModule getBareModule() {
        return linkedModule != null
            ? linkedModule
            : this;
    }

    @Override
    final void finishConstruction(String simpleName, Type declaredType, List<DSLAnnotation> annotations) {
        super.finishConstruction(simpleName, declaredType, annotations);
    }
}
