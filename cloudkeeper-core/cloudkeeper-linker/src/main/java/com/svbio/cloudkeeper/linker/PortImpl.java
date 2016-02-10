package com.svbio.cloudkeeper.linker;

import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.bare.element.module.BareIOPort;
import com.svbio.cloudkeeper.model.bare.element.module.BareInPort;
import com.svbio.cloudkeeper.model.bare.element.module.BareOutPort;
import com.svbio.cloudkeeper.model.bare.element.module.BarePort;
import com.svbio.cloudkeeper.model.bare.element.module.BarePortVisitor;
import com.svbio.cloudkeeper.model.immutable.element.Name;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;
import com.svbio.cloudkeeper.model.runtime.element.RuntimeElement;
import com.svbio.cloudkeeper.model.runtime.element.module.ConnectionKind;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeConnection;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeIOPort;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeInPort;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeModule;
import com.svbio.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import static com.svbio.cloudkeeper.model.runtime.element.module.ConnectionKind.CHILD_OUT_TO_COMPOSITE_OUT;
import static com.svbio.cloudkeeper.model.runtime.element.module.ConnectionKind.COMPOSITE_IN_TO_CHILD_IN;
import static com.svbio.cloudkeeper.model.runtime.element.module.ConnectionKind.SHORT_CIRCUIT;
import static com.svbio.cloudkeeper.model.runtime.element.module.ConnectionKind.SIBLING_CONNECTION;

abstract class PortImpl extends AnnotatedConstructImpl implements IPortImpl, IElementImpl {
    private final SimpleName simpleName;
    private final boolean ownsType;
    private final TypeMirrorImpl type;
    private final int index;
    private final List<ConnectionImpl> inConnections = new ArrayList<>();
    private final List<ConnectionImpl> outConnections = new ArrayList<>();

    @Nullable private IPortContainerImpl enclosingElement;
    private Name qualifiedName;

    /**
     * Constructor for instance that does not own the given type. Instance needs to be explicitly frozen before use.
     *
     * <p>The given type must be frozen outside of this class. It will not be added via
     * {@link #collectEnclosedByAnnotatedConstruct}.
     */
    PortImpl(CopyContext parentContext, SimpleName simpleName, TypeMirrorImpl type, int index) {
        super(State.CREATED, parentContext);
        this.simpleName = Objects.requireNonNull(simpleName);
        ownsType = false;
        this.type = Objects.requireNonNull(type);
        this.index = index;
    }

    /**
     * Copy constructor (from unverified instance). Instance needs to be explicitly frozen before use.
     */
    PortImpl(BarePort original, CopyContext parentContext, int index) throws LinkerException {
        super(original, parentContext);

        CopyContext context = getCopyContext();
        simpleName
            = Preconditions.requireNonNull(original.getSimpleName(), context.newContextForProperty("simpleName"));
        ownsType = true;
        type = TypeMirrorImpl.copyOf(original.getType(), context.newContextForProperty("type"));
        this.index = index;
    }

    private static final class CopyVisitor implements BarePortVisitor<Try<? extends PortImpl>, CopyContext> {
        private final int index;
        private final int inIndex;
        private final int outIndex;

        CopyVisitor(int index, int inIndex, int outIndex) {
            this.index = index;
            this.inIndex = inIndex;
            this.outIndex = outIndex;
        }

        @Override
        public Try<InPortImpl> visitInPort(BareInPort original, @Nullable CopyContext parentContext) {
            assert parentContext != null;
            return Try.run(() -> new InPortImpl(original, parentContext, index, inIndex));
        }

        @Override
        public Try<OutPortImpl> visitOutPort(BareOutPort original, @Nullable CopyContext parentContext) {
            assert parentContext != null;
            return Try.run(() -> new OutPortImpl(original, parentContext, index, outIndex));
        }

        @Override
        public Try<IOPortImpl> visitIOPort(BareIOPort original, @Nullable CopyContext parentContext) {
            assert parentContext != null;
            return Try.run(() -> new IOPortImpl(original, parentContext, index, inIndex, outIndex));
        }
    }

    static PortImpl copyOf(@Nullable BarePort original, CopyContext parentContext, int index, int inIndex, int outIndex)
            throws LinkerException {
        Preconditions.requireNonNull(original, parentContext);
        @Nullable Try<? extends PortImpl> copyTry
            = original.accept(new CopyVisitor(index, inIndex, outIndex), parentContext);
        assert copyTry != null;
        return copyTry.get();
    }

    @Override
    public final int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public boolean equals(Object otherObject) {
        return this == otherObject;
    }

    @Override
    public abstract String toString();

    @Override
    public IElementImpl getSuperAnnotatedConstruct() {
        return null;
    }

    @Override
    @Nonnull
    public final IPortContainerImpl getEnclosingElement() {
        require(State.LINKED);
        assert enclosingElement != null : "must be non-null when in state " + State.LINKED;
        return enclosingElement;
    }

    @Override
    public <T extends RuntimeElement> T getEnclosedElement(Class<T> clazz, SimpleName simpleName) {
        return null;
    }

    @Override
    public ImmutableList<? extends IElementImpl> getEnclosedElements() {
        return ImmutableList.of();
    }

    @Override
    public final Name getQualifiedName() {
        require(State.FINISHED);
        return qualifiedName;
    }

    @Override
    public SimpleName getSimpleName() {
        return simpleName;
    }

    @Override
    public TypeMirrorImpl getType() {
        return type;
    }

    @Override
    public final ModuleImpl getModule() {
        require(State.LINKED);
        assert enclosingElement != null : "must be non-null when in state " + State.LINKED;
        return (ModuleImpl) enclosingElement;
    }

    @Override
    public final int getIndex() {
        return index;
    }

    @Override
    public final ImmutableList<ConnectionImpl> getInConnections() {
        require(State.FINISHED);
        // TODO: Inefficient...
        return ImmutableList.copyOf(inConnections);
    }

    @Override
    public final ImmutableList<ConnectionImpl> getOutConnections() {
        require(State.FINISHED);
        return ImmutableList.copyOf(outConnections);
    }

    /**
     * Adds the given connection that starts from this port.
     */
    @Override
    public final void addOutgoingConnection(ConnectionImpl connection) {
        requireNot(State.FINISHED);
        outConnections.add(connection);
    }

    /**
     * Adds the given connection that end into this port.
     */
    @Override
    public final void addIncomingConnection(ConnectionImpl connection) {
        requireNot(State.FINISHED);
        inConnections.add(connection);
    }

    @Override
    final void collectEnclosedByAnnotatedConstruct(Collection<AbstractFreezable> freezables) {
        if (ownsType) {
            freezables.add(type);
        }
    }

    @Override
    void preProcessFreezable(FinishContext context) {
        enclosingElement = context.getRequiredEnclosingFreezable(IPortContainerImpl.class);
    }

    @Override
    final void finishFreezable(FinishContext context) {
        assert enclosingElement != null : "must be non-null when in state " + State.LINKED;
        qualifiedName = enclosingElement.getQualifiedName().join(simpleName);
    }

    /**
     * Verify port.
     *
     * <p>The <strong>only-one-predecessor rule</strong> states that a port cannot have multiple predecessors, unless
     * the port is of array type and there are multiple connections from element-type ports.
     * Note, however, that an I/O-ports in a loop module may have two incoming connections. For instance, an I/O-port
     * may have one incoming connection from a sibling module (predecessor in first iteration) and one incoming
     * connection from a child module (predecessor in subsequent iterations of the loop). There are also other cases.
     * <p>
     * The following incoming connections are always mutually exclusive by the only-one-predecessor rule. That is,
     * if two mutually exclusive connections existed, then there would be two values at the destination port during
     * execution.
     * <ul>
     *     <li>{@link ConnectionKind#SIBLING_CONNECTION} vs.
     *         {@link ConnectionKind#COMPOSITE_IN_TO_CHILD_IN}</li>
     *     <li>{@link ConnectionKind#CHILD_OUT_TO_COMPOSITE_OUT} vs.
     *         {@link ConnectionKind#SHORT_CIRCUIT}</li>
     * </ul>
     * <p>
     * The following incoming connections are mutually exclusive for pure in- and out-port, but are possible for
     * IO-ports in loop modules. In each case, the first connection provides the initial value for the loop, and the
     * second connection provides the value at the end of a loop iteration.
     * <ul>
     *     <li>
     *         {@link ConnectionKind#SIBLING_CONNECTION} vs.
     *         {@link ConnectionKind#SHORT_CIRCUIT}
     *     </li>
     *     <li>
     *         {@link ConnectionKind#SIBLING_CONNECTION} vs.
     *         {@link ConnectionKind#CHILD_OUT_TO_COMPOSITE_OUT}
     *     </li>
     *     <li>
     *         {@link ConnectionKind#COMPOSITE_IN_TO_CHILD_IN} vs.
     *         {@link ConnectionKind#SHORT_CIRCUIT}<br/>
     *         (here, the loop module is necessarily contained in a composite module)
     *     </li>
     *     <li>
     *         {@link ConnectionKind#COMPOSITE_IN_TO_CHILD_IN} vs.
     *         {@link ConnectionKind#CHILD_OUT_TO_COMPOSITE_OUT}<br/>
     *         (here, the loop module is necessarily contained in a composite module)
     *     </li>
     * </ul>
     * <p>
     * This function does <strong>not</strong> test whether this connection is a variable-argument connection.
     */
    @Override
    final void verifyFreezable(VerifyContext context) throws LinkerException {
        CopyContext copyContext = getCopyContext();

        // Test whether the port has exactly one predecessor. The rule "exactly one predecessor" cannot immediately be
        // checked by testing the number of incoming connections. In particular, if port is an IO-port in a loop module,
        // it may have one incoming connection from a sibling module (predecessor in first iteration), and one incoming
        // connection from a child module (predecessor in subsequent iterations of the loop).
        EnumSet<ConnectionKind> types = EnumSet.noneOf(ConnectionKind.class);
        for (RuntimeConnection connection: getInConnections()) {
            types.add(connection.getKind());
        }

        // The port needs an incoming connection unless it is an out-port (and not an IO-port), its container is not a
        // module, or its enclosing module is a top-level module.
        Preconditions.requireCondition(
            !inConnections.isEmpty()
            || !(this instanceof IInPortImpl)
            || !(enclosingElement instanceof ModuleImpl)
            || ((RuntimeModule) enclosingElement).getParent() == null,
            copyContext,
            "Expected incoming connection into %s of %s.",
            this, enclosingElement
        );

        // Now check
        if (inConnections.size() >= 2) {
            Preconditions.requireCondition(
                this instanceof RuntimeIOPort
                && inConnections.size() == 2
                && types.size() == 2
                && !EnumSet.of(SIBLING_CONNECTION, COMPOSITE_IN_TO_CHILD_IN).equals(types)
                && !EnumSet.of(CHILD_OUT_TO_COMPOSITE_OUT, SHORT_CIRCUIT).equals(types),
                copyContext,
                "Cannot have more than one predecessor for port %s in %s (I/O ports may have two incoming connections "
                    + "if they are not mutually exclusive).",
                this, enclosingElement
            );
        }

        verifyPort(context);
    }

    abstract void verifyPort(VerifyContext context) throws LinkerException;

    static final class IOPortImpl extends PortImpl implements RuntimeIOPort, IInPortImpl, IOutPortImpl {
        private final int inIndex;
        private final int outIndex;

        @Nullable private ImmutableList<IOutPortImpl> dependentOutPorts;
        @Nullable private ImmutableList<IInPortImpl> inPortDependencies;

        IOPortImpl(BarePort original, CopyContext parentContext, int index, int inIndex, int outIndex)
                throws LinkerException {
            super(original, parentContext, index);
            this.inIndex = inIndex;
            this.outIndex = outIndex;
        }

        @Override
        public <T, P> T accept(BarePortVisitor<T, P> visitor, @Nullable P parameter) {
            return visitor.visitIOPort(this, parameter);
        }

        @Override
        public String toString() {
            return BareIOPort.Default.toString(this);
        }

        @Override
        public int getInIndex() {
            return inIndex;
        }

        @Override
        public int getOutIndex() {
            return outIndex;
        }

        @Override
        public ImmutableList<IOutPortImpl> getDependentOutPorts() {
            require(State.FINISHED);
            // No synchronization/volatile needed because reference read/writes are atomic due to JLS §17.7.
            if (dependentOutPorts == null) {
                dependentOutPorts = getEnclosingElement().getOutPorts();
            }
            return dependentOutPorts;
        }

        @Override
        public ImmutableList<? extends RuntimeInPort> getInPortDependencies() {
            require(State.FINISHED);
            // No synchronization/volatile needed because reference read/writes are atomic due to JLS §17.7.
            if (inPortDependencies == null) {
                inPortDependencies = getEnclosingElement().getInPorts();
            }
            return inPortDependencies;
        }

        @Override
        void verifyPort(VerifyContext context) {
            // Warm caches
            getDependentOutPorts();
            getInPortDependencies();
        }
    }

    static final class InPortImpl extends PortImpl implements IInPortImpl {
        private final int inIndex;

        @Nullable private ImmutableList<IOutPortImpl> dependentOutPorts = null;

        InPortImpl(BareInPort original, CopyContext parentContext, int index, int inIndex) throws LinkerException {
            super(original, parentContext, index);
            this.inIndex = inIndex;
        }

        @Override
        public <T, P> T accept(BarePortVisitor<T, P> visitor, @Nullable P parameter) {
            return visitor.visitInPort(this, parameter);
        }

        @Override
        public String toString() {
            return BareInPort.Default.toString(this);
        }

        @Override
        public int getInIndex() {
            return inIndex;
        }

        @Override
        public ImmutableList<IOutPortImpl> getDependentOutPorts() {
            require(State.FINISHED);
            // No synchronization/volatile needed because reference read/writes are atomic due to JLS §17.7.
            if (dependentOutPorts == null) {
                dependentOutPorts = getEnclosingElement().getOutPorts();
            }
            return dependentOutPorts;
        }

        @Override
        void verifyPort(VerifyContext context) {
            // Warm caches
            getDependentOutPorts();
        }
    }

    static final class OutPortImpl extends PortImpl implements IOutPortImpl {
        private final int outIndex;

        @Nullable private ImmutableList<IInPortImpl> inPortDependencies;

        OutPortImpl(CopyContext parentContext, SimpleName name, TypeMirrorImpl type, int index, int outIndex) {
            super(parentContext, name, type, index);
            this.outIndex = outIndex;
        }

        /**
         * Copy constructor (from unverified instance). Instance needs to be explicitly frozen before use.
         */
        OutPortImpl(BareOutPort original, CopyContext parentContext, int index, int outIndex) throws LinkerException {
            super(original, parentContext, index);
            this.outIndex = outIndex;
        }

        @Override
        public <T, P> T accept(BarePortVisitor<T, P> visitor, @Nullable P parameter) {
            return visitor.visitOutPort(this, parameter);
        }

        @Override
        public String toString() {
            return BareOutPort.Default.toString(this);
        }

        @Override
        public int getOutIndex() {
            return outIndex;
        }

        @Override
        public ImmutableList<? extends RuntimeInPort> getInPortDependencies() {
            require(State.FINISHED);
            // No synchronization/volatile needed because reference read/writes are atomic due to JLS §17.7.
            if (inPortDependencies == null) {
                inPortDependencies = getEnclosingElement().getInPorts();
            }
            return inPortDependencies;
        }

        @Override
        void verifyPort(VerifyContext context) {
            // Warm caches
            getInPortDependencies();
        }
    }
}
