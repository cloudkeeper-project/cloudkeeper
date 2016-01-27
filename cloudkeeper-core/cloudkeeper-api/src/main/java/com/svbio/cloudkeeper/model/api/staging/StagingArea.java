package com.svbio.cloudkeeper.model.api.staging;

import akka.japi.Option;
import com.svbio.cloudkeeper.model.api.RuntimeContext;
import com.svbio.cloudkeeper.model.immutable.element.Index;
import com.svbio.cloudkeeper.model.runtime.element.serialization.RuntimeSerializationRoot;
import com.svbio.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;
import com.svbio.cloudkeeper.model.runtime.execution.RuntimeExecutionTrace;
import scala.concurrent.Future;

import javax.annotation.Nullable;

/**
 * CloudKeeper staging area for storing input, output, and intermediate objects.
 *
 * <p>A staging area is a mapping from execution traces to objects. It serves as central place that contains module
 * inputs and outputs, as well as intermediate results. It may be seen as a key-value store where the keys are
 * execution traces ({@link RuntimeExecutionTrace} instances) and the values are arbitrary objects.
 *
 * <p>All methods specified by this interface throw runtime exceptions if the given arguments are null or invalid.
 * Arguments are verified left-to-right, and the order in which exceptions are thrown is {@link NullPointerException},
 * {@link IllegalArgumentException},
 * {@link com.svbio.cloudkeeper.model.runtime.execution.IllegalExecutionTraceException}.
 *
 * <p>Implementations of this interface are required to be thread-safe. Moreover, implementations must support the
 * concurrent invocation of staging-area methods provided the calls are <em>consistent</em> (that is,
 * non-contradictory). Invocations are contradictory if
 * <ul><li>
 *     the staging-area instances have a common ancestor with respect to
 *     {@link #resolveDescendant(RuntimeExecutionTrace)} (or are the same) <em>and</em>
 * </li><li>
 *     at least one invocation is not {@link #delete(RuntimeExecutionTrace)}.
 * </li></ul>
 * The effect of concurrently invoking staging-area methods that are not consistent is undefined. In particular, this
 * includes running concurrent staging-area operations across multiple JVMs.
 *
 * <p>Even across multiple JVMs, staging areas provide a consistent view if operations are run sequentially (that is, if
 * a staging-area operation on one JVM is only started once no staging-area operation is running on any other JVM any
 * more).
 *
 * <p>All methods throw {@link NullPointerException} if {@code null} is passed to a parameter not annotated with
 * {@link Nullable}.
 */
public interface StagingArea {
    /**
     * Returns the annotated absolute execution trace for this staging area.
     */
    RuntimeAnnotatedExecutionTrace getAnnotatedExecutionTrace();

    /**
     * Deletes all entries with keys that start with the given execution trace (inclusively).
     *
     * <p>The given prefix must have a non-empty call stack or a non-empty value reference; that is, either
     * {@link RuntimeExecutionTrace#getFrames()} or {@link RuntimeExecutionTrace#getReference()} must be non-empty.
     * The prefix must not contain array indices.
     *
     * <p>The returned future will be completed successfully if the given prefix does not match any entries.
     *
     * @param prefix Execution trace prefix with non-empty call stack or non-empty value reference that does not contain
     *     array indices.
     * @return Future completed with {@code prefix} on success and an {@link StagingException} on failure
     *     (unless the {@link Throwable} is not an {@link Exception}).
     * @throws IllegalArgumentException if the argument does not satisfy the constraints described above
     * @throws com.svbio.cloudkeeper.model.runtime.execution.IllegalExecutionTraceException if the given execution trace
     *     is not valid relative to the absolute execution trace represented by this staging area
     */
    Future<RuntimeExecutionTrace> delete(RuntimeExecutionTrace prefix);

    /**
     * Writes the object for the given source execution trace also as object for the given target execution trace.
     *
     * <p>Both the {@code source} and {@code target} execution trace must include a value reference. That is, they must
     * include a port, and optionally they may include array indices following the port.
     *
     * <p>Implementations are encouraged to implement this method without performing a physical copy. For instance, an
     * implementation may simply make the existing value available under an additional key (similar to "hard links" in
     * a file system).
     *
     * @param source source execution trace, {@link RuntimeExecutionTrace#getReference()} must be non-empty
     * @param target target execution trace, {@link RuntimeExecutionTrace#getReference()} must be non-empty
     * @return Future completed with {@code target} on success and an {@link StagingException} on failure
     *     (unless the {@link Throwable} is not an {@link Exception}).
     * @throws IllegalArgumentException if the arguments do not satisfy the constraints described above
     * @throws com.svbio.cloudkeeper.model.runtime.execution.IllegalExecutionTraceException if any of the two given
     *     execution traces is not valid relative to the absolute execution trace represented by this staging area
     */
    Future<RuntimeExecutionTrace> copy(RuntimeExecutionTrace source, RuntimeExecutionTrace target);

    /**
     * Writes an object for the given execution trace.
     *
     * <p>The {@code target} execution trace must include a value reference. That is, it must include a port, and
     * optionally it may include array indices following the port.
     *
     * <p>A staging area may store objects in an arbitrary (but self-contained) way that allows later retrieval with
     * {@link #getObject(RuntimeExecutionTrace)}. For instance, a simple staging-area implementation might store objects
     * in a map in memory, whereas another implementation might store objects by serializing them to the file system.
     *
     * <p>If serialization is used, staging-area implementations are suggested to call
     * {@link RuntimeAnnotatedExecutionTrace#getSerializationDeclarations()} and then proceed with serialization as
     * documented for that method. In case an exception occurs during serialization, it must be available as cause of
     * the {@link StagingException} that the returned future will be completed with.
     *
     * @param target target execution trace, {@link RuntimeExecutionTrace#getReference()} must be non-empty
     * @param object object that is to be written
     *
     * @return Future completed with {@code target} on success and a {@link StagingException} on failure
     *     (unless the {@link Throwable} is not an {@link Exception}).
     * @throws IllegalArgumentException if the arguments do not satisfy the constraints described above
     * @throws com.svbio.cloudkeeper.model.runtime.execution.IllegalExecutionTraceException if the given execution trace
     *     is not valid relative to the absolute execution trace represented by this staging area
     */
    Future<RuntimeExecutionTrace> putObject(RuntimeExecutionTrace target, Object object);

    /**
     * Writes an object, represented as serialization tree, for the given execution trace.
     *
     * <p>This method is similar to {@link #putObject(RuntimeExecutionTrace, Object)}, but it takes a persistence-tree
     * representation of the object that is to be written. This method should (only) be called if the object itself is
     * not available.
     *
     * <p>If the staging area does not store serialized representations, this method may deserialize the serialization
     * tree and store the deserialized value instead. In case an exception occurs during deserialization, it will be
     * available as cause of the {@link StagingException} that the returned future will be completed with.
     *
     * @param target target execution trace, {@link RuntimeExecutionTrace#getReference()} must be non-empty
     * @param serializationTree serialization tree that is to be written
     *
     * @return Future completed with {@code target} on success and a {@link StagingException} on failure
     *     (unless the {@link Throwable} is not an {@link Exception}).
     * @throws IllegalArgumentException if the arguments do not satisfy the constraints described above
     * @throws com.svbio.cloudkeeper.model.runtime.execution.IllegalExecutionTraceException if the given execution trace
     *     is not valid relative to the absolute execution trace represented by this staging area
     */
    Future<RuntimeExecutionTrace> putSerializationTree(RuntimeExecutionTrace target,
        RuntimeSerializationRoot serializationTree);

    /**
     * Returns the object for the given execution trace.
     *
     * @param source source execution trace, {@link RuntimeExecutionTrace#getReference()} must be non-empty
     * @return Future completed with object on success and an {@link StagingException} on failure
     *     (unless the {@link Throwable} is not an {@link Exception}).
     * @throws IllegalArgumentException if the argument does not satisfy the constraints described above
     * @throws com.svbio.cloudkeeper.model.runtime.execution.IllegalExecutionTraceException if the given execution trace
     *     is not valid relative to the absolute execution trace represented by this staging area
     */
    Future<Object> getObject(RuntimeExecutionTrace source);

    /**
     * Returns whether a value exists at an execution trace.
     *
     * @param source source execution trace, {@link RuntimeExecutionTrace#getReference()} must be non-empty
     * @return Future completed with {@code true} (if a value exists) or {@code false} (if no value exists at the given
     *     trace) on success and an {@link StagingException} on failure (unless the {@link Throwable} is
     *     not an {@link Exception}).
     * @throws IllegalArgumentException if the argument does not satisfy the constraints described above
     * @throws com.svbio.cloudkeeper.model.runtime.execution.IllegalExecutionTraceException if the given execution trace
     *     is not valid relative to the absolute execution trace represented by this staging area
     */
    Future<Boolean> exists(RuntimeExecutionTrace source);

    /**
     * Returns the maximum index present at the given execution trace that is smaller than or equal to the given upper
     * bound.
     *
     * @param trace relative execution trace, must be of type {@link RuntimeExecutionTrace.Type#CONTENT} and
     *     {@link RuntimeExecutionTrace#getReference()} must be empty
     * @param upperBound upper bound on the index that will be returned; may be null if there is no upper bound
     * @return Future completed with the maximum index (or an empty {@link Option} if no index exists) on success and a
     *     {@link StagingException} on failure (unless the {@link Throwable} is not an {@link Exception}).
     * @throws IllegalArgumentException if the arguments do not satisfy the constraints described above
     * @throws com.svbio.cloudkeeper.model.runtime.execution.IllegalExecutionTraceException if the given execution trace
     *     is not valid relative to the absolute execution trace represented by this staging area
     */
    Future<Option<Index>> getMaximumIndex(RuntimeExecutionTrace trace, @Nullable Index upperBound);

    /**
     * Returns a staging area for the given descendant execution trace.
     *
     * @param trace execution trace, must be of type
     *     {@link RuntimeExecutionTrace.Type#MODULE} or
     *     {@link RuntimeExecutionTrace.Type#ITERATION} and have an empty
     *     value reference
     * @return a staging instance for the given descendant execution trace
     * @throws IllegalArgumentException if the argument does not satisfy the constraints described above
     * @throws com.svbio.cloudkeeper.model.runtime.execution.IllegalExecutionTraceException if the given execution trace
     *     is not valid relative to the absolute execution trace represented by this staging area
     */
    StagingArea resolveDescendant(RuntimeExecutionTrace trace);

    /**
     * Returns a provider for staging-area instances rooted at this execution trace.
     *
     * <p>The purpose of this method is to allow transferring state to a different machine. While staging-area
     * implementations are not serializable in general, providers are. Their serialized representation typically
     * contains information specific to an implementation. For instance, for a file-based implementation the root file
     * path would be part of the serialized representation.
     *
     * <p>In order to reconstruct a staging area, the provider may need additional context beyond the "embedded"
     * parameters. This context is accessible through the {@link InstanceProvider} that
     * is passed to
     * {@link StagingAreaProvider#provideStaging(RuntimeContext, RuntimeAnnotatedExecutionTrace, InstanceProvider)}.
     *
     * <p>This method may only be called if:
     * <ul><li>
     *     the absolute execution trace represented by this staging area is empty, or
     * </li><li>
     *     the the execution trace has type {@link RuntimeExecutionTrace.Type#MODULE} and
     *     {@link RuntimeAnnotatedExecutionTrace#getModule()} references a simple module.
     * </li></ul>
     *
     * @return staging provider instance
     * @throws IllegalStateException if the absolute execution trace represented by this staging area is neither
     *     (a) empty nor (b) of type {@link RuntimeExecutionTrace.Type#MODULE} and
     *     {@link RuntimeAnnotatedExecutionTrace#getModule()} references a simple module.
     * @throws UnsupportedOperationException if this staging area does not support use from different JVMs
     */
    StagingAreaProvider getStagingAreaProvider();
}
