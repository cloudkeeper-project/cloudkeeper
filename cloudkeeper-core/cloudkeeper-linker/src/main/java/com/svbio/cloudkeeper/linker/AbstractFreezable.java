package com.svbio.cloudkeeper.linker;

import com.svbio.cloudkeeper.linker.CopyContext.CopyContextSupplier;
import com.svbio.cloudkeeper.linker.CopyContext.ListPropertyCopyContext;
import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.bare.element.module.BarePort;
import com.svbio.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Skeletal abstract base class for instances with a lifecycle, which starts in a building phase, and eventually
 * transitions to an (effectively) immutable phase.
 *
 * <p>Prior to reaching their immutable phase, objects are not exposed outside of this package (unless explicitly
 * documented and explained in a subclass). The complete lifecycle is defined by the {@link State} enumeration.
 * Instances transition to the next phase in their lifecycle when one of the {@link #linkProxyModules}, {@link #finish},
 * or {@link #verify} methods is called.
 *
 * <p>Instances of this class form a logical containment hierarchy, and during a phase transition every instance is
 * expected to trigger a corresponding phase transition also in all instances that it contains. Subclasses must
 * implement {@link #collectEnclosed} in order to achieve this behavior. It is important to note that each
 * instance of this class must be contained by exactly one enclosing instance; except for the root instance, which does
 * not have a container. The {@link #complete} is expected to be called on the root instance in order to reach the
 * effectively immutable phase for the entire object containment hierarchy.
 */
abstract class AbstractFreezable {
    enum State {
        CREATED,
        LINKED,

        /**
         * An instance in this state is ready to be used. All interface methods are expected to give expected results
         * that are immutable and consistent. However, some methods may trigger on-the-fly computations (as opposed to
         * just returning cached results).
         */
        FINISHED,
        PRECOMPUTED
    }

    private volatile State state = State.CREATED;

    // No need to be volatile because instance variable is only accessed before object is verified, which will always
    // be from a single thread.
    @Nullable private CopyContext copyContext;

    AbstractFreezable(State initialState, @Nullable CopyContext parentContext) {
        state = initialState;
        if (parentContext != null) {
            copyContext = parentContext.newSystemContext(String.format("fresh instance of %s", getClass()));
        }
    }

    AbstractFreezable(@Nullable Object original, CopyContext parentContext) throws LinkerException {
        Preconditions.requireNonNull(original, parentContext);
        copyContext = parentContext.newContextForChild(original);
    }

    CopyContext getCopyContext() {
        requireNot(State.PRECOMPUTED);
        if (copyContext == null) {
            throw new IllegalStateException("getCopyContext() called on object created without copy context.");
        }
        return copyContext;
    }

    /**
     * Returns the state of this instance.
     */
    State getState() {
        return state;
    }

    private List<AbstractFreezable> getEnclosedFreezables() {
        List<AbstractFreezable> freezables = new ArrayList<>();
        collectEnclosed(freezables);
        return freezables;
    }

    /**
     * Adds all freezable objects directly enclosed by this object to the given list.
     *
     * <p>Subclasses need to override this method if they contain freezable members. Subclasses must
     * <strong>not</strong> call this method first. Instead, a subclass must only override this method with a final
     * method. A subclass may, however, in turn define and call a new empty-bodied method that its own subclasses may
     * override.
     *
     * @param freezables list to add directly enclosed freezable objects to
     */
    abstract void collectEnclosed(Collection<AbstractFreezable> freezables);

    /**
     * Throws an {@link IllegalStateException} if this instance is not in or after the given state.
     *
     * @throws IllegalStateException if this instance is not in or after the given state
     */
    final void require(State inOrAfterState) {
        if (state.compareTo(inOrAfterState) < 0) {
            throw new IllegalStateException(String.format(
                "Expected instance of %s to be at least in state %s, but current state is %s.",
                getClass(), inOrAfterState, state
            ));
        }
    }

    /**
     * Throws an {@link IllegalStateException} if this instance is not before the given state.
     *
     * @throws IllegalStateException if this instance is not before the given state
     */
    final void requireNot(State beforeState) {
        if (state.compareTo(beforeState) >= 0) {
            throw new IllegalStateException(String.format(
                "Expected instance of %s to be in state before %s, but current state is %s.",
                getClass(), beforeState, state
            ));
        }
    }

    /**
     * Resolves the ports in all {@link ProxyModuleImpl} and {@link ConnectionImpl} instances.
     *
     * <p>Just the {@link #finish(FinishContext)} method is not enough for the following reason. We need to make two passes
     * over our descendants: First, add ports to all {@link ProxyModuleImpl} instances. Only then, we can finish
     * construction. Otherwise, we run into a cyclic dependency:
     * <ul><li>
     *     (a) determine apply-to-all connections in proxy module ->
     * </li><li>
     *     (b) no pending connections in parent composite module ->
     * </li><li>
     *     (c) ports in proxy module need to be present
     * </li></ul>
     * Hence, if (a), (b), and (c) happened in the same method (say, in
     * {@link #finish(FinishContext)}), then the proxy module would depend on its parent, and the parent
     * would depend on its proxy-module child.
     *
     * @param context context for linking proxy modules
     */
    final void linkProxyModules(FinishContext context) throws LinkerException {
        requireNot(State.LINKED);
        preProcessFreezable(context);
        state = State.LINKED;

        FinishContext contextForContained = context.newChildContext(this);
        for (AbstractFreezable freezable: getEnclosedFreezables()) {
            freezable.linkProxyModules(contextForContained);
        }
    }

    abstract void preProcessFreezable(FinishContext context) throws LinkerException;

    /**
     * Finish construction of this instance by resolving by-name references into Java object references.
     *
     * <p>This method also updates other internal state and performs verification as they pertain to finishing the
     * object creation. Not all model errors can be detected as this point, yet.
     *
     * @param context context for finishing construction
     */
    final void finish(FinishContext context) throws LinkerException  {
        require(State.LINKED);
        requireNot(State.FINISHED);
        finishFreezable(context);
        state = State.FINISHED;

        FinishContext contextForContained = context.newChildContext(this);
        for (AbstractFreezable freezable: getEnclosedFreezables()) {
            freezable.finish(contextForContained);
        }
    }

    abstract void finishFreezable(FinishContext context) throws LinkerException;

    final void verify(VerifyContext context) throws LinkerException {
        require(State.FINISHED);
        requireNot(State.PRECOMPUTED);
        verifyFreezable(context);
        state = State.PRECOMPUTED;
        copyContext = null;

        for (AbstractFreezable freezable: getEnclosedFreezables()) {
            freezable.verify(context);
        }
    }

    abstract void verifyFreezable(VerifyContext context) throws LinkerException;

    /**
     * Manually mark this instance as frozen/ready.
     */
    final void markAsCompleted() {
        requireNot(State.LINKED);
        copyContext = null;
        state = State.PRECOMPUTED;
    }

    final void complete(FinishContext finishContext, VerifyContext verifyContext) throws LinkerException {
        linkProxyModules(finishContext);
        finish(finishContext);
        verify(verifyContext);
    }

    @FunctionalInterface
    interface LinkedConstructor<T, U> {
        U map(T original, CopyContext copyContext) throws LinkerException;
    }

    @FunctionalInterface
    interface KeyExtractor<T,K> {
        K apply(T original);
    }

    @FunctionalInterface
    interface Accumulator<T> {
        void apply(T element, CopyContext elementCopyContext) throws LinkerException;
    }

    static <T, U> void collect(List<T> list, ListPropertyCopyContext contextForListProperty,
            LinkedConstructor<T, U> constructor, Collection<Accumulator<U>> accumulators) throws LinkerException {
        CopyContextSupplier contextSupplier = contextForListProperty.supplier();
        for (T element : list) {
            CopyContext elementCopyContext = contextSupplier.get();
            U newElement = constructor.map(element, elementCopyContext);
            for (Accumulator<U> accumulator: accumulators) {
                accumulator.apply(newElement, elementCopyContext);
            }
        }
    }

    <T, U> void collect(List<T> list, String listProperty,
            LinkedConstructor<T, U> constructor, Collection<Accumulator<U>> accumulators) throws LinkerException {
        collect(list, getCopyContext().newContextForListProperty(listProperty), constructor, accumulators);
    }

    static <K, V> Accumulator<V> mapAccumulator(Map<? super K, ? super V> map, KeyExtractor<V, K> keyExtractor) {
        return (element, elementCopyContext) -> {
            K key = keyExtractor.apply(element);
            @Nullable Object previous = map.put(key, element);
            Preconditions.requireCondition(previous == null, elementCopyContext,
                "Multiple elements with the same key '%s'.", key);
        };
    }

    static <V> Accumulator<V> listAccumulator(List<V> mutableList) {
        return (element, elementCopyContext) -> mutableList.add(element);
    }

    final <T, V> ImmutableList<V> immutableListOf(List<T> list, String listProperty,
            LinkedConstructor<T, V> constructor) throws LinkerException {
        List<V> newList = new ArrayList<>(list.size());
        collect(
            list,
            listProperty,
            constructor,
            Collections.singletonList(listAccumulator(newList))
        );
        return ImmutableList.copyOf(newList);
    }

    final <T, K, V> Map<K, V> unmodifiableMapOf(List<T> list, String listProperty, LinkedConstructor<T, V> constructor,
            KeyExtractor<V, K> keyExtractor) throws LinkerException {
        LinkedHashMap<K, V> map = new LinkedHashMap<>(list.size());
        collect(
            list,
            getCopyContext().newContextForListProperty(listProperty),
            constructor,
            Collections.singletonList(mapAccumulator(map, keyExtractor))
        );
        return Collections.unmodifiableMap(map);
    }

    static final class PortAccumulationState {
        private final List<PortImpl> allPorts = new ArrayList<>();
        private final List<IInPortImpl> inPorts = new ArrayList<>();
        private final List<IOutPortImpl> outPorts = new ArrayList<>();

        List<PortImpl> getAllPorts() {
            return allPorts;
        }

        List<IInPortImpl> getInPorts() {
            return inPorts;
        }

        List<IOutPortImpl> getOutPorts() {
            return outPorts;
        }
    }

    static <T extends BarePort> LinkedConstructor<T, PortImpl> portConstructor(PortAccumulationState state) {
        return (originalPort, elementCopyContext)
            -> PortImpl.copyOf(originalPort, elementCopyContext, state.allPorts.size(), state.getInPorts().size(),
            state.outPorts.size());
    }

    static Accumulator<PortImpl> portAccumulator(PortAccumulationState state) {
        return (newPort, elementCopyContext) -> {
            int numListsAddedTo = 0;
            if (newPort instanceof IInPortImpl) {
                ++numListsAddedTo;
                state.inPorts.add((IInPortImpl) newPort);
            }
            if (newPort instanceof IOutPortImpl) {
                ++numListsAddedTo;
                state.outPorts.add((IOutPortImpl) newPort);
            }
            assert numListsAddedTo > 0 : "Detected port that is neither in- nor out-port (or both)";

            state.allPorts.add(newPort);
        };
    }
}
