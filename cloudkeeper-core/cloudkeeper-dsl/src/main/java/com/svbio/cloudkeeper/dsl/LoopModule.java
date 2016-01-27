package com.svbio.cloudkeeper.dsl;

import com.svbio.cloudkeeper.model.bare.element.module.BareIOPort;
import com.svbio.cloudkeeper.model.bare.element.module.BareLoopModule;
import com.svbio.cloudkeeper.model.bare.element.module.BareModuleVisitor;
import com.svbio.cloudkeeper.model.bare.element.module.BareOutPort;
import com.svbio.cloudkeeper.model.bare.element.module.BarePortVisitor;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;

import java.lang.reflect.Type;
import java.util.List;

public abstract class LoopModule<D extends LoopModule<D>> extends DSLParentModule<D> implements BareLoopModule {
    final class ContinuePort extends ToConnectablePort<Boolean> implements DSLOutPort<Boolean> {
        ContinuePort() {
            super(SimpleName.identifier(BareLoopModule.CONTINUE_PORT_NAME), Boolean.class, true, null);
        }

        @Override
        public String toString() {
            return BareOutPort.Default.toString(this);
        }

        @Override
        public <T, P> T accept(BarePortVisitor<T, P> visitor, P parameter) {
            return visitor.visitOutPort(this, parameter);
        }
    }

    private final ContinuePort continuePort = new ContinuePort();

    /**
     * Constructor.
     *
     * This class is meant to be subclassed, so its constructor is public.
     */
    protected LoopModule() {
        createPorts(new PortVisitor() {
            @Override
            public void visitPortClass(SimpleName name, Class<?> portClass, Type type,
                List<DSLAnnotation> annotations) {

                if (InPort.class.isAssignableFrom(portClass)) {
                    new InPort<>(name, type, annotations);
                } else if (OutPort.class.isAssignableFrom(portClass)) {
                    new OutPort<>(name, type, annotations);
                } else if (IOPort.class.isAssignableFrom(portClass)) {
                    new IOPort<>(name, type, annotations);
                }
            }
        });
    }

    @Override
    public final String toString() {
        return BareLoopModule.Default.toString(this);
    }

    /**
     * I/O-port that both takes input and produces output.
     *
     * @param <T> The type of the I/O-port
     */
    public final class IOPort<T> extends ToConnectablePort<T>
        implements FromConnectable<T>, DSLOutPort<T>, DSLInPort<T>, BareIOPort {

        IOPort(SimpleName name, Type type, List<DSLAnnotation> annotations) {
            super(name, type, false, annotations);
        }

        @Override
        public String toString() {
            return BareIOPort.Default.toString(this);
        }

        @Override
        public <U, P> U accept(BarePortVisitor<U, P> visitor, P parameter) {
            return visitor.visitIOPort(this, parameter);
        }
    }

    protected final LoopModule<D> repeatIf(FromConnectable<? extends Boolean> fromPort) {
        requireUnfrozen();

        continuePort.from(fromPort);
        return this;
    }

    @Override
    public final <T, P> T accept(BareModuleVisitor<T, P> visitor, P parameter) {
        return visitor.visitLoopModule(this, parameter);
    }

    @Override
    void finishConstruction(String simpleName, Type declaredType, List<DSLAnnotation> annotations) {
        super.finishConstruction(simpleName, declaredType, annotations);
        // Implicit ports are not handled by super.finishConstruction()
        continuePort.finishConstruction();
    }
}
