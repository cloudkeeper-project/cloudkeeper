package com.svbio.cloudkeeper.model.runtime.execution;

import com.svbio.cloudkeeper.model.Immutable;
import com.svbio.cloudkeeper.model.bare.execution.BareExecutionTrace;
import com.svbio.cloudkeeper.model.immutable.element.Index;
import com.svbio.cloudkeeper.model.immutable.element.Key;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;
import com.svbio.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;

public interface RuntimeExecutionTrace extends BareExecutionTrace, Immutable {
    /**
     * Type of an execution-trace element.
     *
     * The type of an execution trace is the type of its last element.
     */
    enum Type {
        ARRAY_INDEX,
        IN_PORT,
        OUT_PORT,
        MODULE,
        ITERATION,
        CONTENT
    }

    /**
     * Calls the visitor method that is appropriate for the type of this execution trace.
     *
     * @param visitor execution-trace visitor
     * @param <T> return type of visitor
     * @param <P> parameter type
     * @return result returned by visitor
     */
    @Nullable
    <T, P> T accept(RuntimeExecutionTraceVisitor<T, P> visitor, @Nullable P parameter);

    /**
     * Returns whether this execution trace does not contain any elements.
     *
     * This method is a convenience method and equivalent to {@code asElementList().isEmpty()}.
     */
    boolean isEmpty();

    /**
     * Returns the number of elements in this execution trace.
     *
     * This method is a convenience method and equivalent to {@code asElementList().size()}.
     */
    int size();

    /**
     * Returns an unmodifiable list containing the elements of this execution trace.
     *
     * If this execution trace is empty, an empty list is returned. If this execution trace contains a single element,
     * a singleton list, consisting of only the current execution trace, is returned.
     */
    ImmutableList<? extends RuntimeExecutionTrace> asElementList();

    /**
     * Returns the type of the last element of this trace, or {@link Type#MODULE} is this trace is an absolute empty
     * trace.
     *
     * @throws IndexOutOfBoundsException if this trace is empty and not an absolute trace
     */
    Type getType();

    /**
     * Returns the key (either a simple name or an index) at the last position of this trace.
     *
     * If the type of the last element of this execution trace is known, the specialized methods
     * {@link #getSimpleName()} and {@link #getIndex()} may be used.
     *
     * @return key at the last position of this trace
     * @throws IndexOutOfBoundsException if this trace is empty
     * @throws ClassCastException if the the last element of this trace is {@link Type#CONTENT}
     */
    Key getKey();

    /**
     * Returns the simple name at the last position of this trace.
     *
     * @return simple name at the last position of this trace
     * @throws IndexOutOfBoundsException if this trace is empty
     * @throws ClassCastException if the the last element of this trace is neither {@link Type#MODULE},
     *     {@link Type#IN_PORT}, nor {@link Type#OUT_PORT}
     */
    SimpleName getSimpleName();

    /**
     * Returns the index at the last position of this trace.
     *
     * @return index at the last position of this trace
     * @throws IndexOutOfBoundsException if this trace is empty
     * @throws ClassCastException if the the last element of this trace is neither {@link Type#ITERATION} nor
     *     {@link Type#ARRAY_INDEX}
     */
    Index getIndex();

    /**
     * Returns the subtrace that only contains elements of types {@link Type#CONTENT}, {@link Type#MODULE}, or
     * {@link Type#ITERATION}.
     */
    RuntimeExecutionTrace getFrames();

    /**
     * Returns the subtrace that starts with the port element, or returns the empty trace if this trace does not contain
     * a port element.
     */
    RuntimeExecutionTrace getReference();

    /**
     * Returns an execution trace that contains the current execution trace appended by the given (relative) execution
     * trace.
     *
     * @param trace relative execution trace
     * @throws IllegalExecutionTraceException if the resulting execution trace would not be valid as specified by
     *     {@link com.svbio.cloudkeeper.model.bare.execution.BareExecutionTrace}
     */
    RuntimeExecutionTrace resolveExecutionTrace(RuntimeExecutionTrace trace);

    /**
     * Returns an execution trace that contains the current execution trace appended by an element representing the
     * given module.
     *
     * @param moduleName name of the module
     * @throws IllegalExecutionTraceException if the resulting execution trace would not be valid as specified by
     *     {@link BareExecutionTrace}
     */
    RuntimeExecutionTrace resolveModule(SimpleName moduleName);

    /**
     * Returns an execution trace that contains the current execution trace appended by a content element.
     *
     * @throws IllegalExecutionTraceException if the resulting execution trace would not be valid as specified by
     *     {@link BareExecutionTrace}
     */
    RuntimeExecutionTrace resolveContent();

    /**
     * Returns an execution trace that contains the current execution trace appended by an element representing
     * the given iteration.
     *
     * @throws IllegalExecutionTraceException if the resulting execution trace would not be valid as specified by
     *     {@link BareExecutionTrace}
     */
    RuntimeExecutionTrace resolveIteration(Index index);

    /**
     * Returns an execution trace that contains the current execution trace appended by an element representing
     * the given array index.
     *
     * @throws IllegalExecutionTraceException if the resulting execution trace would not be valid as specified by
     *     {@link BareExecutionTrace}
     */
    RuntimeExecutionTrace resolveArrayIndex(Index index);

    /**
     * Returns an trace that starts with the current execution trace and also contains the given in-port.
     *
     * @throws IllegalExecutionTraceException if the resulting execution trace would not be valid as specified by
     *     {@link BareExecutionTrace}
     */
    RuntimeExecutionTrace resolveInPort(SimpleName inPortName);

    /**
     * Returns an trace that starts with the current execution trace and also contains the given in-port.
     *
     * @throws IllegalExecutionTraceException if the resulting execution trace would not be valid as specified by
     *     {@link BareExecutionTrace}
     */
    RuntimeExecutionTrace resolveOutPort(SimpleName outPortName);

    /**
     * Returns a trace that is a subsequence of the elements of this trace.
     *
     * @param beginIndex the index of the first element, inclusive
     * @param endIndex the index of the last element, exclusive
     * @return a new trace that is a subsequence of the elements of this trace
     * @throws IndexOutOfBoundsException If beginIndex is negative, or greater than or equal to the number of elements.
     *     If endIndex is less than or equal to beginIndex, or larger than the number of elements.
     */
    RuntimeExecutionTrace subtrace(int beginIndex, int endIndex);
}
