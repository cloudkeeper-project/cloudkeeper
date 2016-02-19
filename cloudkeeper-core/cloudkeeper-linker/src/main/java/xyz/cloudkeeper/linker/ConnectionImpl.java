package xyz.cloudkeeper.linker;

import xyz.cloudkeeper.model.LinkerException;
import xyz.cloudkeeper.model.bare.element.module.BareChildOutToParentOutConnection;
import xyz.cloudkeeper.model.bare.element.module.BareConnection;
import xyz.cloudkeeper.model.bare.element.module.BareConnectionVisitor;
import xyz.cloudkeeper.model.bare.element.module.BareParentInToChildInConnection;
import xyz.cloudkeeper.model.bare.element.module.BareShortCircuitConnection;
import xyz.cloudkeeper.model.bare.element.module.BareSiblingConnection;
import xyz.cloudkeeper.model.runtime.element.module.ConnectionKind;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeChildOutToParentOutConnection;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeConnection;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeConnectionVisitor;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeParentInToChildInConnection;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeParentModule;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeShortCircuitConnection;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeSiblingConnection;
import xyz.cloudkeeper.model.runtime.element.module.TypeRelationship;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

import static xyz.cloudkeeper.model.runtime.element.module.TypeRelationship.APPLY_TO_ALL;
import static xyz.cloudkeeper.model.runtime.element.module.TypeRelationship.INCOMPATIBLE;
import static xyz.cloudkeeper.model.runtime.element.module.TypeRelationship.MERGE;

abstract class ConnectionImpl extends LocatableImpl implements RuntimeConnection {
    private final SimpleNameReference fromPortReference;
    private final SimpleNameReference toPortReference;
    private final int index;

    private ParentModuleImpl parent;
    @Nullable private TypeRelationship typeRelationship;

    private ConnectionImpl(BareConnection original, CopyContext parentContext, int index)
            throws LinkerException {
        super(original, parentContext);

        CopyContext context = getCopyContext();
        fromPortReference = new SimpleNameReference(original.getFromPort(), context.newContextForProperty("fromPort"));
        toPortReference = new SimpleNameReference(original.getToPort(), context.newContextForProperty("toPort"));
        this.index = index;
    }

    final SimpleNameReference getFromPortReference() {
        return fromPortReference;
    }

    final SimpleNameReference getToPortReference() {
        return toPortReference;
    }

    private static final class CopyVisitor
            implements BareConnectionVisitor<Try<? extends ConnectionImpl>, CopyContext> {
        private final int index;

        private CopyVisitor(int index) {
            this.index = index;
        }

        @Override
        @Nullable
        public Try<Sibling> visitSiblingConnection(BareSiblingConnection connection,
                @Nullable CopyContext parentContext) {
            assert parentContext != null;
            return Try.run(() -> new Sibling(connection, parentContext, index));
        }

        @Override
        @Nullable
        public Try<ParentInToChildIn> visitParentInToChildInConnection(
                BareParentInToChildInConnection connection, @Nullable CopyContext parentContext) {
            assert parentContext != null;
            return Try.run(() -> new ParentInToChildIn(connection, parentContext, index));
        }

        @Override
        @Nullable
        public Try<ChildOutToParentOut> visitChildOutToParentOutConnection(
                BareChildOutToParentOutConnection connection, @Nullable CopyContext parentContext) {
            assert parentContext != null;
            return Try.run(() -> new ChildOutToParentOut(connection, parentContext, index));
        }

        @Override
        @Nullable
        public Try<ShortCircuit> visitShortCircuitConnection(BareShortCircuitConnection original,
                @Nullable CopyContext parentContext) {
            assert parentContext != null;
            return Try.run(() -> new ShortCircuit(original, parentContext, index));
        }
    }

    static ConnectionImpl copyOf(BareConnection original, CopyContext parentContext, int index)
            throws LinkerException {
        Preconditions.requireNonNull(original, parentContext);
        @Nullable Try<? extends ConnectionImpl> copyTry = original.accept(new CopyVisitor(index), parentContext);
        assert copyTry != null;
        return copyTry.get();
    }

    private static final class Sibling extends ConnectionImpl implements RuntimeSiblingConnection {
        private final SimpleNameReference fromModuleReference;
        private final SimpleNameReference toModuleReference;
        private IOutPortImpl fromPort;
        private IInPortImpl toPort;

        private Sibling(BareSiblingConnection original, CopyContext parentContext, int index)
                throws LinkerException {
            super(original, parentContext, index);
            CopyContext context = getCopyContext();
            fromModuleReference
                = new SimpleNameReference(original.getFromModule(), context.newContextForProperty("fromModule"));
            toModuleReference
                = new SimpleNameReference(original.getToModule(), context.newContextForProperty("toModule"));
        }

        @Override
        @Nullable
        public <T, P> T accept(BareConnectionVisitor<T, P> visitor, @Nullable P parameter) {
            return visitor.visitSiblingConnection(this, parameter);
        }

        @Override
        @Nullable
        public <T, P> T accept(RuntimeConnectionVisitor<T, P> visitor, @Nullable P parameter) {
            return visitor.visitSiblingConnection(this, parameter);
        }

        @Override
        public String toString() {
            return BareSiblingConnection.Default.toString(this);
        }

        @Override
        public ConnectionKind getKind() {
            return ConnectionKind.SIBLING_CONNECTION;
        }

        @Override
        public IOutPortImpl getFromPort() {
            require(State.LINKED);
            return fromPort;
        }

        @Override
        public IInPortImpl getToPort() {
            require(State.LINKED);
            return toPort;
        }

        @Override
        void collectEnclosed(Collection<AbstractFreezable> freezables) {
            freezables.add(fromModuleReference);
            freezables.add(toModuleReference);
        }

        @Override
        void preProcessConnection(FinishContext context) throws LinkerException {
            fromPort = context.getChildOutPort(fromModuleReference, getFromPortReference());
            toPort = context.getChildInPort(toModuleReference, getToPortReference());
            fromPort.addOutgoingConnection(this);
            toPort.addIncomingConnection(this);
        }
    }

    private static final class ParentInToChildIn extends ConnectionImpl implements RuntimeParentInToChildInConnection {
        private final SimpleNameReference toModuleReference;
        private IInPortImpl fromPort;
        private IInPortImpl toPort;

        private ParentInToChildIn(BareParentInToChildInConnection original,
            CopyContext parentContext, int index) throws LinkerException  {
            super(original, parentContext, index);
            toModuleReference
                = new SimpleNameReference(original.getToModule(), getCopyContext().newContextForProperty("toModule"));
        }

        @Override
        @Nullable
        public <T, P> T accept(BareConnectionVisitor<T, P> visitor, @Nullable P parameter) {
            return visitor.visitParentInToChildInConnection(this, parameter);
        }

        @Override
        @Nullable
        public <T, P> T accept(RuntimeConnectionVisitor<T, P> visitor, @Nullable P parameter) {
            return visitor.visitParentInToChildInConnection(this, parameter);
        }

        @Override
        public String toString() {
            return BareParentInToChildInConnection.Default.toString(this);
        }

        @Override
        public ConnectionKind getKind() {
            return ConnectionKind.COMPOSITE_IN_TO_CHILD_IN;
        }

        @Override
        public IInPortImpl getFromPort() {
            require(State.LINKED);
            return fromPort;
        }

        @Override
        public IInPortImpl getToPort() {
            require(State.LINKED);
            return toPort;
        }

        @Override
        void collectEnclosed(Collection<AbstractFreezable> freezables) {
            freezables.add(toModuleReference);
        }

        @Override
        void preProcessConnection(FinishContext context) throws LinkerException {
            fromPort = context.getParentInPort(getFromPortReference());
            toPort = context.getChildInPort(toModuleReference, getToPortReference());
            fromPort.addOutgoingConnection(this);
            toPort.addIncomingConnection(this);
        }
    }

    private static final class ChildOutToParentOut
            extends ConnectionImpl
            implements RuntimeChildOutToParentOutConnection {
        private final SimpleNameReference fromModuleReference;
        private IOutPortImpl fromPort;
        private IOutPortImpl toPort;

        private ChildOutToParentOut(BareChildOutToParentOutConnection original,
            CopyContext parentContext, int index) throws LinkerException {
            super(original, parentContext, index);
            fromModuleReference = new SimpleNameReference(
                original.getFromModule(),
                getCopyContext().newContextForProperty("fromModule")
            );
        }

        @Override
        @Nullable
        public <T, P> T accept(BareConnectionVisitor<T, P> visitor, @Nullable P parameter) {
            return visitor.visitChildOutToParentOutConnection(this, parameter);
        }

        @Override
        @Nullable
        public <T, P> T accept(RuntimeConnectionVisitor<T, P> visitor, @Nullable P parameter) {
            return visitor.visitChildOutToParentOutConnection(this, parameter);
        }

        @Override
        public String toString() {
            return BareChildOutToParentOutConnection.Default.toString(this);
        }

        @Override
        public ConnectionKind getKind() {
            return ConnectionKind.CHILD_OUT_TO_COMPOSITE_OUT;
        }

        @Override
        public IOutPortImpl getFromPort() {
            require(State.LINKED);
            return fromPort;
        }

        @Override
        public IOutPortImpl getToPort() {
            require(State.LINKED);
            return toPort;
        }

        @Override
        void collectEnclosed(Collection<AbstractFreezable> freezables) {
            freezables.add(fromModuleReference);
        }

        @Override
        void preProcessConnection(FinishContext context) throws LinkerException {
            fromPort = context.getChildOutPort(fromModuleReference, getFromPortReference());
            toPort = context.getParentOutPort(getToPortReference());
            fromPort.addOutgoingConnection(this);
            toPort.addIncomingConnection(this);
        }
    }

    private static final class ShortCircuit extends ConnectionImpl implements RuntimeShortCircuitConnection {
        private IInPortImpl fromPort;
        private IOutPortImpl toPort;

        private ShortCircuit(BareShortCircuitConnection original, CopyContext parentContext,
            int index) throws LinkerException {
            super(original, parentContext, index);
        }

        @Override
        @Nullable
        public <T, P> T accept(BareConnectionVisitor<T, P> visitor, @Nullable P parameter) {
            return visitor.visitShortCircuitConnection(this, parameter);
        }

        @Override
        @Nullable
        public <T, P> T accept(RuntimeConnectionVisitor<T, P> visitor, @Nullable P parameter) {
            return visitor.visitShortCircuitConnection(this, parameter);
        }

        @Override
        public String toString() {
            return BareShortCircuitConnection.Default.toString(this);
        }

        @Override
        public ConnectionKind getKind() {
            return ConnectionKind.SHORT_CIRCUIT;
        }

        @Override
        public IInPortImpl getFromPort() {
            require(State.LINKED);
            return fromPort;
        }

        @Override
        public IOutPortImpl getToPort() {
            require(State.LINKED);
            return toPort;
        }

        @Override
        void collectEnclosed(Collection<AbstractFreezable> freezables) { }

        @Override
        void preProcessConnection(FinishContext context) throws LinkerException {
            fromPort = context.getParentInPort(getFromPortReference());
            toPort = context.getParentOutPort(getToPortReference());
            fromPort.addOutgoingConnection(this);
            toPort.addIncomingConnection(this);
        }
    }

    @Override
    public RuntimeParentModule getParentModule() {
        require(State.LINKED);
        return parent;
    }

    @Override
    public ModuleImpl getFromModule() {
        return getFromPort().getModule();
    }

    @Override
    @Nonnull
    public abstract IPortImpl getFromPort();

    @Override
    public ModuleImpl getToModule() {
        return getToPort().getModule();
    }

    @Override
    @Nonnull
    public abstract IPortImpl getToPort();

    @Override
    public TypeRelationship getTypeRelationship() {
        if (typeRelationship == null) {
            // We can only do this after the connection is frozen.
            typeRelationship = getFromPort().getType().relationshipTo(getToPort().getType());
        }
        return typeRelationship;
    }

    @Override
    public int getIndex() {
        return index;
    }

    /**
     * Resolves the {@link PortImpl} references and registers the connections with the {@link PortImpl}
     * instances.
     *
     * <p>This methods must run before the {@link ParentModuleImpl#finish(FinishContext)} method of the enclosing
     * parent module is called.
     *
     * @see ProxyModuleImpl#preProcessFreezable(FinishContext)
     * @see ParentModuleImpl#collectEnclosedByAnnotatedConstruct(Collection)
     *
     * @throws xyz.cloudkeeper.model.NotFoundException if modules or ports cannot be resolved
     */
    @Override
    final void preProcessFreezable(FinishContext context) throws LinkerException {
        parent = context.getRequiredEnclosingFreezable(ParentModuleImpl.class);
        preProcessConnection(context);
    }

    abstract void preProcessConnection(FinishContext context) throws LinkerException;

    @Override
    final void finishFreezable(FinishContext context) { }

    @Override
    final void verifyFreezable(VerifyContext context) throws LinkerException {
        CopyContext copyContext = getCopyContext();

        IPortImpl fromPort = getFromPort();
        ModuleImpl fromModule = fromPort.getModule();
        IPortImpl toPort = getToPort();
        ModuleImpl toModule = toPort.getModule();

        // Precompute type relationship
        getTypeRelationship();

        // If the connection is not short-circuit, then source and target module cannot be one and the same
        Preconditions.requireCondition(this instanceof ShortCircuit || fromModule != toModule,
            copyContext, "Connection from %s to %s in %s is invalid, because it is not a short-circuit question.",
            fromPort, toPort, toModule
        );

        // Apply-to-all modules place some constraints. See {@link TypeRelationship#APPLY_TO_ALL}
        Preconditions.requireCondition(typeRelationship != INCOMPATIBLE, copyContext,
            "Connection from %s in %s to %s in %s is invalid because of incompatible types. Cannot link from %s to %s.",
            fromPort, fromModule, toPort, toModule, fromPort.getType(), toPort.getType()
        );

        if (typeRelationship == APPLY_TO_ALL) {
            Preconditions.requireCondition(this == toModule.getApplyToAllConnection(), copyContext,
                "Modules cannot have multiple apply-to-all connections. Previous connection: %s",
                toModule.getApplyToAllConnection());
            Preconditions.requireCondition(
                this instanceof ParentInToChildIn || this instanceof Sibling,
                copyContext,
                "Connection from %s in %s to %s in %s is not a valid apply-to-all connection. Source and "
                    + "destination modules must be siblings, or the destination module must be a child of the "
                    + "source module.", fromPort, fromModule, toPort, toModule
            );
        } else if (typeRelationship == MERGE) {
            Preconditions.requireCondition(
                this instanceof ChildOutToParentOut || this instanceof Sibling,
                copyContext,
                "Connection from %s in %s to %s in % is not a valid combine-into-array connection. Source and "
                    + "destination modules must be siblings, or the destination module must be the parent of the "
                    + "source module.", fromPort, fromModule, toPort, toModule
            );
        }

        // If the source module is apply-to-all, then all outgoing connections (this connection included), need to be
        // combine-into-array connections.
        Preconditions.requireCondition(fromModule.getApplyToAllConnection() == null || typeRelationship == MERGE,
            copyContext,
            "Connection from %s in %s to %s in %s is not a combine-into-array connection. Outgoing connections "
                + "from out-ports of an apply-to-all module must be combine-into-array connections.",
            fromPort, fromModule, toPort, toModule
        );
    }
}
