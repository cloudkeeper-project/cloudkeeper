package xyz.cloudkeeper.model.runtime.execution;

import xyz.cloudkeeper.model.immutable.element.Index;
import xyz.cloudkeeper.model.immutable.element.Name;
import xyz.cloudkeeper.model.immutable.element.SimpleName;
import xyz.cloudkeeper.model.runtime.element.annotation.RuntimeAnnotation;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeInPort;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeModule;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeOutPort;
import xyz.cloudkeeper.model.runtime.element.serialization.RuntimeSerializationDeclaration;
import xyz.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;

/**
 * Absolute execution trace.
 *
 * <p>An absolute execution trace always has a module as its first element.
 */
public interface RuntimeAnnotatedExecutionTrace extends RuntimeExecutionTrace {
    /**
     * Human-readable name of the entity modeled by this interface.
     */
    String NAME = "absolute execution trace";

    /**
     * Returns the module represented by the last element of this execution trace, or the module of the parent trace if
     * the last element of this execution trace does not represent a module (recursive definition).
     *
     * <p>If the current module is a proxy module referencing a composite module declaration, then a
     * {@link xyz.cloudkeeper.model.runtime.element.module.RuntimeProxyModule} instance is returned by this
     * method.
     *
     * @return Module represented by the last element of this execution trace, or the module of the parent trace if the
     *     last element of this execution trace does not represent a module (recursive definition). The return value is
     *     guaranteed to be non-null because the first element of an absolute execution trace always represents a
     *     module.
     */
    RuntimeModule getModule();

    /**
     * Returns the in-port represented by the first element of the value reference of this execution trace.
     *
     * <p>The result of this method is equal to the result of this method when called on the first element of the
     * execution trace returned by {@link #getReference()}.
     *
     * <p>Note that only a limited view of the entire workflow may be available. For instance, if the enclosing module
     * of the current module is not available, then the returned {@link RuntimeInPort} will not contain any incoming
     * connections.
     *
     * @return the in-port represented by the first element of the value reference of this execution trace
     * @throws IllegalStateException If this execution trace does not contain an in-port; that is, if the type returned
     *     by {@link #getType()} for the first element of {@link #getReference()} is not equal to {@link Type#IN_PORT}.
     */
    RuntimeInPort getInPort();

    /**
     * Returns the out-port represented by the first element of the value reference of this execution trace.
     *
     * <p>The result of this method is equal to the result of this method when called on the first element of the
     * execution trace returned by {@link #getReference()}.
     *
     * <p>Note that only a limited view of the entire workflow may be available. For instance, if the enclosing module
     * of the current module is not available, then the returned {@link RuntimeOutPort} will not contain any outgoing
     * connections.
     *
     * @return the out-port represented by the first element of the value reference of this execution trace
     * @throws IllegalStateException If this execution trace does not contain an out-port; that is, if the type returned
     *     by {@link #getType()} for the first element of {@link #getReference()} is not equal to {@link Type#OUT_PORT}.
     */
    RuntimeOutPort getOutPort();

    /**
     * Returns the annotation overrides.
     *
     * This method correspond to {@link xyz.cloudkeeper.model.bare.execution.BareExecutable#getOverrides()} in the
     * bare model. Implementations are free to prune the list of overrides by removing those that cannot apply to
     * this absolute execution trace or absolute execution traces that have this execution trace as prefix.
     *
     * @see xyz.cloudkeeper.model.bare.execution.BareExecutable#getOverrides()
     */
    ImmutableList<? extends RuntimeOverride> getOverrides();

    /**
     * Returns this execution trace's annotation of the specified type if such an annotation is <em>present</em>, else
     * null.
     *
     * <p>An annotation <em>A</em> with annotation type <em>AT</em> is <em>directly present</em> on an execution trace
     * <em>E</em> if <em>A</em> is part of an annotation override for <em>E</em>. <em>A</em> is <em>directly
     * present</em> on an annotated construct <em>C</em> if
     * <ul><li>
     *     <em>A</em> is part of an annotation override for <em>C</em>
     * </li><li>
     *     No annotation of type <em>AT</em> is part of an annotation override for <em>C</em>, and <em>A</em> is
     *     explicitly declared as applying to <em>C</em>
     *     (that is, <em>A</em> is contained in the list returned by
     *     {@link xyz.cloudkeeper.model.runtime.element.RuntimeAnnotatedConstruct#getDeclaredAnnotations()} for
     *     that annotated construct <em>C</em>).
     * </li></li>/ul>
     *
     * <p>An annotation <em>A</em> with annotation type <em>AT</em> is <em>present</em> on an execution trace <em>E</em>
     * representing annotated construct <em>C</em> if either:
     * <ul><li>
     *     <em>A</em> is directly present on <em>E</em>.
     * </li><li>
     *     No annotation of type <em>AT</em> is directly present on <em>E</em>, and <em>A</em> is directly present on
     *     <em>C</em>.
     * </li><li>
     *     No annotation of type <em>AT</em> is directly present on <em>E</em> nor <em>C</em>, and <em>AT</em> is
     *     inheritable, and <em>A</em> is present on the super annotated construct of <em>C</em>.
     * </li></ul>
     *
     * <p>An annotation <em>A</em> is <em>present</em> on an execution trace <em>E</em> that does not represent an
     * annotated element if <em>A</em> is present on the nearest enclosing execution trace representing an annotated
     * element. For instance, the annotations present on execution trace {@code /module:in:array:1:2} are those
     * present on {@code /module:in:array}. Note that if <em>E</em> does not represent an annotated element, it always
     * has an enclosing annotated element, because the first execution-trace element (not to be confused with annotation
     * element) is always a module.
     *
     * @param annotationTypeName the name of the annotation type
     * @return this execution trace' annotation of the specified annotation type if present, else null
     */
    @Nullable
    RuntimeAnnotation getAnnotation(Name annotationTypeName);

    /**
     * Returns this execution trace's annotation of the specified type if such an annotation is <em>present</em>, else
     * null.
     *
     * <p>This method is equivalent to calling {@link RuntimeAnnotation#getJavaAnnotation(Class)} on the result of
     * {@code getAnnotation(Name.valueOf(annotation.getName()))}.
     *
     * @param annotation {@link Class} object corresponding to the annotation type
     * @param <A> the annotation type
     * @return this execution trace' annotation for the specified annotation type if present, else null
     */
    @Nullable
    <A extends Annotation> A getAnnotation(Class<A> annotation);

    /**
     * Returns the (ordered) list of serialization declarations that should be used for serializing the Java objects
     * at this execution trace.
     *
     * The list returned will contain the concatenation of the following lists, where duplicates are omitted:
     * <ul><li>
     *     The serialization plug-ins specified by the
     *     {@link cloudkeeper.annotations.CloudKeeperSerialization} annotation on this execution trace, as returned by
     *     {@link #getAnnotation(Name)}.
     * </li><li>
     *     The serialization plug-ins specified by the {@link cloudkeeper.annotations.CloudKeeperSerialization}
     *     annotation on the type plug-ins corresponding to the type erasure of the type of the port corresponding to
     *     this execution trace. The type plug-ins are returned by
     *     {@link xyz.cloudkeeper.model.runtime.type.RuntimeTypeMirror#asTypeDeclaration()}.
     * </li><li>
     *     The serialization plug-ins specified by the {@link cloudkeeper.annotations.CloudKeeperSerialization}
     *     annotation on all serialization plug-ins added in this and the previous two stages (this stage may therefore
     *     add serialization plug-ins recursively).
     * </li><li>
     *     The default serialization declarations (that are part of the CloudKeeper system repository). As opposed to
     *     the previous stages, dependencies will not automatically be added in this stage.
     * </li></ul>
     *
     * @return list of serialization declarations that should be used for serializing the Java objects
     *     at this execution trace
     * @throws IllegalStateException if this execution trace has an empty value reference; that is, no enclosing trace
     *     that represents a port
     */
    ImmutableList<? extends RuntimeSerializationDeclaration> getSerializationDeclarations();

    @Override
    RuntimeAnnotatedExecutionTrace resolveExecutionTrace(RuntimeExecutionTrace trace);

    @Override
    RuntimeAnnotatedExecutionTrace resolveModule(SimpleName moduleName);

    @Override
    RuntimeAnnotatedExecutionTrace resolveIteration(Index index);

    @Override
    RuntimeAnnotatedExecutionTrace resolveContent();

    @Override
    RuntimeAnnotatedExecutionTrace resolveInPort(SimpleName inPortName);

    @Override
    RuntimeAnnotatedExecutionTrace resolveOutPort(SimpleName outPortName);

    @Override
    RuntimeAnnotatedExecutionTrace resolveArrayIndex(Index index);
}
