package com.svbio.cloudkeeper.linker;

import cloudkeeper.annotations.CloudKeeperSerialization;
import com.svbio.cloudkeeper.linker.CopyContext.CopyContextSupplier;
import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.bare.element.annotation.BareAnnotationTypeElement;
import com.svbio.cloudkeeper.model.bare.element.module.BareModule;
import com.svbio.cloudkeeper.model.bare.execution.BareExecutionTrace;
import com.svbio.cloudkeeper.model.bare.execution.BareOverride;
import com.svbio.cloudkeeper.model.immutable.element.Index;
import com.svbio.cloudkeeper.model.immutable.element.Key;
import com.svbio.cloudkeeper.model.immutable.element.Name;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;
import com.svbio.cloudkeeper.model.immutable.execution.ExecutionTrace;
import com.svbio.cloudkeeper.model.runtime.execution.IllegalExecutionTraceException;
import com.svbio.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;
import com.svbio.cloudkeeper.model.runtime.execution.RuntimeElementPatternTarget;
import com.svbio.cloudkeeper.model.runtime.execution.RuntimeElementTarget;
import com.svbio.cloudkeeper.model.runtime.execution.RuntimeExecutionTrace;
import com.svbio.cloudkeeper.model.runtime.execution.RuntimeExecutionTracePatternTarget;
import com.svbio.cloudkeeper.model.runtime.execution.RuntimeExecutionTraceTarget;
import com.svbio.cloudkeeper.model.runtime.execution.RuntimeExecutionTraceVisitor;
import com.svbio.cloudkeeper.model.runtime.execution.RuntimeOverrideTargetVisitor;
import com.svbio.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

final class AnnotatedExecutionTraceImpl extends AbstractFreezable implements RuntimeAnnotatedExecutionTrace {
    private final ExecutionTrace executionTrace;
    private final IElementImpl element;
    private final ImmutableList<OverrideImpl> overrides;

    private ImmutableList<SerializationDeclarationImpl> defaultSerializationDeclarations;

    /**
     * Constructor for effectively immutable instances (already frozen at construction time).
     */
    AnnotatedExecutionTraceImpl(ExecutionTrace executionTrace, IElementImpl element,
            List<OverrideImpl> overrides,
            List<SerializationDeclarationImpl> defaultSerializationDeclarations) {
        super(State.PRECOMPUTED, null);
        assert !executionTrace.isEmpty() || element instanceof ModuleImpl;

        this.executionTrace = executionTrace;
        this.element = element;
        this.overrides = ImmutableList.copyOf(overrides);
        this.defaultSerializationDeclarations = ImmutableList.copyOf(defaultSerializationDeclarations);
    }

    /**
     * Constructor (from unverified module and overrides). Instance needs to be explicitly frozen before use.
     *
     * @param executionTrace the (not necessarily annotated) execution trace
     * @param module the module
     * @param overrides overrides
     * @param parentContext parent copy context
     */
    AnnotatedExecutionTraceImpl(BareExecutionTrace executionTrace, BareModule module,
            List<? extends BareOverride> overrides, CopyContext parentContext) throws LinkerException {
        super(RuntimeAnnotatedExecutionTrace.NAME, parentContext);
        Objects.requireNonNull(executionTrace);
        Objects.requireNonNull(module);
        Objects.requireNonNull(overrides);

        CopyContext context = getCopyContext();
        this.executionTrace = ExecutionTrace.copyOf(executionTrace);
        element = ModuleImpl.copyOf(module, context.newContextForProperty("module"), -1);

        CopyContextSupplier overrideContextSupplier = context.newContextForListProperty("overrides").supplier();
        List<OverrideImpl> newOverrides = new ArrayList<>(overrides.size());
        for (BareOverride override: overrides) {
             newOverrides.add(new OverrideImpl(override, overrideContextSupplier.get()));
        }
        this.overrides = ImmutableList.copyOf(newOverrides);
    }

    @Override
    public String toString() {
        return executionTrace.toString();
    }

    @Override
    public <T, P> T accept(RuntimeExecutionTraceVisitor<T, P> visitor, P parameter) {
        return executionTrace.accept(visitor, parameter);
    }

    @Override
    public boolean isEmpty() {
        return executionTrace.isEmpty();
    }

    @Override
    public int size() {
        return executionTrace.size();
    }

    @Override
    public ImmutableList<ExecutionTrace> asElementList() {
        return executionTrace.asElementList();
    }

    @Override
    public Key getKey() {
        return executionTrace.getKey();
    }

    @Override
    public SimpleName getSimpleName() {
        return executionTrace.getSimpleName();
    }

    @Override
    public Index getIndex() {
        return executionTrace.getIndex();
    }

    @Override
    public Type getType() {
        // An absolute empty execution trace must be of type MODULE.
        return executionTrace.isEmpty()
            ? Type.MODULE
            : executionTrace.getType();
    }

    @Override
    public RuntimeExecutionTrace getFrames() {
        return executionTrace.getFrames();
    }

    @Override
    public RuntimeExecutionTrace getReference() {
        return executionTrace.getReference();
    }

    @Override
    public RuntimeExecutionTrace subtrace(int beginIndex, int endIndex) {
        return executionTrace.subtrace(beginIndex, endIndex);
    }

    @Override
    public ModuleImpl getModule() {
        return element instanceof PortImpl
            ? ((IPortImpl) element).getModule()
            : (ModuleImpl) element;
    }

    @Override
    public PortImpl.InPortImpl getInPort() {
        if (!(element instanceof PortImpl.InPortImpl)) {
            throw new IllegalStateException(String.format(
                "Execution trace '%s' does not represent an in-port.", executionTrace
            ));
        }
        return (PortImpl.InPortImpl) element;
    }

    @Override
    public PortImpl.OutPortImpl getOutPort() {
        if (!(element instanceof PortImpl.OutPortImpl)) {
            throw new IllegalStateException(String.format(
                "Execution trace '%s' does not represent an out-port.", executionTrace
            ));
        }
        return (PortImpl.OutPortImpl) element;
    }

    @Override
    public ImmutableList<OverrideImpl> getOverrides() {
        return overrides;
    }

    private enum MatchExecutionTraceVisitor implements RuntimeOverrideTargetVisitor<Boolean, ExecutionTrace> {
        INSTANCE;

        @Override
        public Boolean visitElementTarget(RuntimeElementTarget target, @Nullable ExecutionTrace current) {
            return Boolean.FALSE;
        }

        @Override
        public Boolean visitElementPatternTarget(RuntimeElementPatternTarget target, @Nullable ExecutionTrace current) {
            return Boolean.FALSE;
        }

        @Override
        public Boolean visitExecutionTraceTarget(RuntimeExecutionTraceTarget target, @Nullable ExecutionTrace current) {
            return ExecutionTrace.copyOf(target.getExecutionTrace()).equals(current);
        }

        @Override
        public Boolean visitExecutionTracePatternTarget(RuntimeExecutionTracePatternTarget target,
                @Nullable ExecutionTrace current) {
            assert current != null;
            return target.getPattern().matcher(current.toString()).matches();
        }
    }

    private enum MatchElementVisitor implements RuntimeOverrideTargetVisitor<Boolean, Name> {
        INSTANCE;

        @Override
        public Boolean visitElementTarget(RuntimeElementTarget target, @Nullable Name current) {
            assert current != null;
            return target.getElement().getQualifiedName().equals(current);
        }

        @Override
        public Boolean visitElementPatternTarget(RuntimeElementPatternTarget target, @Nullable Name current) {
            assert current != null;
            return target.getPattern().matcher(current.toString()).matches();
        }

        @Override
        public Boolean visitExecutionTraceTarget(RuntimeExecutionTraceTarget target, @Nullable Name current) {
            return Boolean.FALSE;
        }

        @Override
        public Boolean visitExecutionTracePatternTarget(RuntimeExecutionTracePatternTarget target,
                @Nullable Name current) {
            return Boolean.FALSE;
        }
    }

    @Nullable
    private <P> AnnotationImpl findAnnotation(Name annotationTypeName,
            RuntimeOverrideTargetVisitor<Boolean, P> visitor, P parameter) {
        @Nullable AnnotationImpl annotation = null;
        for (OverrideImpl override: overrides) {
            for (OverrideTargetImpl target: override.getTargets()) {
                if (Boolean.TRUE.equals(target.accept(visitor, parameter))) {
                    annotation = override.getDeclaredAnnotation(annotationTypeName);

                    // Break out of target loop: One target was a match, so no need to check the other targets of this
                    // override any more.
                    break;
                }
            }
            // The current execution trace or element may be matched by multiple overrides. In this case, the latest
            // override has precedence. Hence, no break here.
        }
        return annotation;
    }

    /**
     * Returns the given element's annotation of the specified type if such an annotation is <em>present</em>, else
     * null.
     *
     * <p>See {@link RuntimeAnnotatedExecutionTrace#getAnnotation(Name)} for definition of <em>declared</em> and
     * <em>present</em> annotations.
     *
     * @param element element whose annotation is requested
     * @param annotationTypeName the name of the annotation type
     * @return the given element's annotation of the specified annotation type if present, else null
     */
    @Nullable
    private AnnotationImpl getAnnotationForElement(IElementImpl element, Name annotationTypeName) {
        @Nullable IElementImpl currentElement = element;
        @Nullable AnnotationImpl annotation;
        do {
            annotation = findAnnotation(
                annotationTypeName, MatchElementVisitor.INSTANCE, currentElement.getQualifiedName());
            if (annotation == null) {
                annotation = currentElement.getDeclaredAnnotation(annotationTypeName);
            }
            currentElement = currentElement.getSuperAnnotatedConstruct();
        } while (annotation == null && currentElement != null);

        return annotation;
    }

    @Override
    @Nullable
    public AnnotationImpl getAnnotation(Name annotationTypeName) {
        ExecutionTrace currentExecutionTrace = executionTrace;
        @Nullable AnnotationImpl annotation = null;
        while (annotation == null) {
            annotation = findAnnotation(annotationTypeName, MatchExecutionTraceVisitor.INSTANCE, currentExecutionTrace);

            // Special case: An absolute empty execution trace always represents a MODULE. However, ExecutionTrace
            // represents a potentially relative execution trace, so getType() cannot be called on an empty
            // ExecutionTrace.
            if (currentExecutionTrace.isEmpty()
                    || EnumSet.of(Type.MODULE, Type.IN_PORT, Type.OUT_PORT).contains(currentExecutionTrace.getType())) {
                break;
            }
            currentExecutionTrace = currentExecutionTrace.subtrace(0, currentExecutionTrace.asElementList().size() - 1);
        }

        if (annotation == null) {
            // There was no override of the annotation for the current execution trace.

            annotation = getAnnotationForElement(element, annotationTypeName);
        }

        return annotation;
    }

    @Override
    @Nullable
    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        Name annotationTypeName = Name.qualifiedName(annotationType.getName());
        @Nullable AnnotationImpl annotation = getAnnotation(annotationTypeName);
        return annotation != null
            ? annotation.getJavaAnnotation(annotationType)
            : null;
    }

    /**
     * Adds to the given set the serialization plugin referenced by the given annotation.
     *
     * <p>The given annotation must be {@code null} or of type {@link CloudKeeperSerialization}.
     */
    private static void addSerializationDeclarations(Set<SerializationDeclarationImpl> set,
            @Nullable AnnotationImpl annotation) {
        if (annotation != null) {
            assert annotation.getDeclaration().getQualifiedName()
                .contentEquals(CloudKeeperSerialization.class.getName());

            @Nullable AnnotationValueImpl annotationValue = annotation.getValue(
                SimpleName.identifier(BareAnnotationTypeElement.DEFAULT_SIMPLE_NAME));
            if (annotationValue != null) {
                @SuppressWarnings("unchecked")
                List<AnnotationValueImpl> list = (List<AnnotationValueImpl>) annotationValue.getValue();
                for (AnnotationValueImpl listElement: list) {
                    // TODO: Type check
                    set.add((SerializationDeclarationImpl) listElement.getElement());
                }
            }
        }
    }

    @Override
    public ImmutableList<SerializationDeclarationImpl> getSerializationDeclarations() {
        require(State.FINISHED);
        if (executionTrace.getReference().asElementList().isEmpty()) {
            throw new IllegalStateException(String.format(
                "getSerializationDeclarations() called for execution trace '%s', which does not contain a port.",
                executionTrace
            ));
        }

        Name annotationTypeName = Name.qualifiedName(CloudKeeperSerialization.class.getName());
        Set<SerializationDeclarationImpl> set = new LinkedHashSet<>();

        // Marshaler declarations need to be added for annotations present on:
        // 1. the current execution trace
        addSerializationDeclarations(set, getAnnotation(annotationTypeName));

        // 2. type plug-ins corresponding to the port type
        PortImpl port = (PortImpl) element;
        for (TypeDeclarationImpl typeDeclaration: port.getType().asTypeDeclaration()) {
            addSerializationDeclarations(set, getAnnotationForElement(typeDeclaration, annotationTypeName));
        }

        // 3. Marshaler plug-ins referenced by other serialization plug-ins in the list
        Set<SerializationDeclarationImpl> oldSet = set;
        do {
            Set<SerializationDeclarationImpl> newSet = new LinkedHashSet<>();
            for (SerializationDeclarationImpl serializationDeclaration: oldSet) {
                addSerializationDeclarations(
                    newSet,
                    getAnnotationForElement(serializationDeclaration, annotationTypeName)
                );
            }
            set.addAll(newSet);
            oldSet = newSet;
        } while (!oldSet.isEmpty());

        // 4. default/fall-back serialization declarations (a linker option)
        set.addAll(defaultSerializationDeclarations);

        return ImmutableList.copyOf(set);
    }

    @Override
    public AnnotatedExecutionTraceImpl resolveModule(SimpleName moduleName) {
        require(State.FINISHED);
        ExecutionTrace newExecutionTrace = executionTrace.resolveModule(moduleName);

        @Nullable ParentModuleImpl parentModule = null;
        if (element instanceof ProxyModuleImpl) {
            ModuleDeclarationImpl declaration = ((ProxyModuleImpl) element).getDeclaration();
            if (declaration instanceof CompositeModuleDeclarationImpl) {
                parentModule = ((CompositeModuleDeclarationImpl) declaration).getTemplate();
            }
        } else if (element instanceof ParentModuleImpl) {
            parentModule = (ParentModuleImpl) element;
        }

        if (parentModule == null) {
            throw new IllegalStateException(String.format(
                "Expected parent module at execution trace '%s', but got %s.", executionTrace, element
            ));
        }

        @Nullable ModuleImpl newModule = parentModule.getModule(moduleName);
        if (newModule == null) {
            throw new IllegalExecutionTraceException(String.format(
                "Could not find child module with name '%s' within parent module at execution trace '%s'.",
                moduleName, executionTrace
            ));
        }

        return new AnnotatedExecutionTraceImpl(newExecutionTrace, newModule, overrides,
            defaultSerializationDeclarations);
    }

    @Override
    public AnnotatedExecutionTraceImpl resolveIteration(Index index) {
        require(State.FINISHED);
        return new AnnotatedExecutionTraceImpl(executionTrace.resolveIteration(index), element, overrides,
            defaultSerializationDeclarations);
    }

    @Override
    public AnnotatedExecutionTraceImpl resolveContent() {
        require(State.FINISHED);
        return new AnnotatedExecutionTraceImpl(executionTrace.resolveContent(), element, overrides,
            defaultSerializationDeclarations);
    }

    @Override
    public AnnotatedExecutionTraceImpl resolveInPort(SimpleName inPortName) {
        require(State.FINISHED);
        ExecutionTrace newExecutionTrace = executionTrace.resolveInPort(inPortName);

        ModuleImpl module = (ModuleImpl) element;
        @Nullable PortImpl inPort = module.getEnclosedElement(PortImpl.class, inPortName);
        if (!(inPort instanceof IInPortImpl)) {
            throw new IllegalExecutionTraceException(String.format(
                "Could not find in-port with name '%s' within module at execution trace '%s'.",
                inPortName, executionTrace
            ));
        }

        return new AnnotatedExecutionTraceImpl(newExecutionTrace, inPort, overrides,
            defaultSerializationDeclarations);
    }

    @Override
    public AnnotatedExecutionTraceImpl resolveOutPort(SimpleName outPortName) {
        require(State.FINISHED);
        ExecutionTrace newExecutionTrace = executionTrace.resolveOutPort(outPortName);

        ModuleImpl module = (ModuleImpl) element;
        @Nullable PortImpl outPort = module.getEnclosedElement(PortImpl.class, outPortName);
        if (!(outPort instanceof IOutPortImpl)) {
            throw new IllegalExecutionTraceException(String.format(
                "Could not find out-port with name '%s' within module at execution trace '%s'.",
                outPortName, executionTrace
            ));
        }

        return new AnnotatedExecutionTraceImpl(newExecutionTrace, outPort, overrides,
            defaultSerializationDeclarations);
    }

    @Override
    public AnnotatedExecutionTraceImpl resolveArrayIndex(Index index) {
        require(State.FINISHED);
        return new AnnotatedExecutionTraceImpl(executionTrace.resolveArrayIndex(index), element, overrides,
            defaultSerializationDeclarations);
    }

    private enum ResolveVisitor
            implements RuntimeExecutionTraceVisitor<AnnotatedExecutionTraceImpl, AnnotatedExecutionTraceImpl> {
        INSTANCE;

        @Override
        public AnnotatedExecutionTraceImpl visitModule(RuntimeExecutionTrace moduleElement,
                AnnotatedExecutionTraceImpl oldTrace) {
            return oldTrace.resolveModule(moduleElement.getSimpleName());
        }

        @Override
        public AnnotatedExecutionTraceImpl visitContent(RuntimeExecutionTrace contentElement,
                AnnotatedExecutionTraceImpl oldTrace) {
            return oldTrace.resolveContent();
        }

        @Override
        public AnnotatedExecutionTraceImpl visitIteration(RuntimeExecutionTrace iterationElement,
                AnnotatedExecutionTraceImpl oldTrace) {
            return oldTrace.resolveIteration(iterationElement.getIndex());
        }

        @Override
        public AnnotatedExecutionTraceImpl visitInPort(RuntimeExecutionTrace inPortElement,
                AnnotatedExecutionTraceImpl oldTrace) {
            return oldTrace.resolveInPort(inPortElement.getSimpleName());
        }

        @Override
        public AnnotatedExecutionTraceImpl visitOutPort(RuntimeExecutionTrace outPortElement,
                AnnotatedExecutionTraceImpl oldTrace) {
            return oldTrace.resolveOutPort(outPortElement.getSimpleName());
        }

        @Override
        public AnnotatedExecutionTraceImpl visitArrayIndex(RuntimeExecutionTrace arrayIndexElement,
                AnnotatedExecutionTraceImpl oldTrace) {
            return oldTrace.resolveArrayIndex(arrayIndexElement.getIndex());
        }
    }

    @Override
    public AnnotatedExecutionTraceImpl resolveExecutionTrace(RuntimeExecutionTrace trace) {
        AnnotatedExecutionTraceImpl newTrace = this;
        for (RuntimeExecutionTrace traceElement: trace.asElementList()) {
            newTrace = traceElement.accept(ResolveVisitor.INSTANCE, newTrace);
        }
        return newTrace;
    }

    @Override
    void collectEnclosed(Collection<AbstractFreezable> freezables) {
        freezables.add((AbstractFreezable) element);
        freezables.addAll(overrides);
    }

    @Override
    void preProcessFreezable(FinishContext context) { }

    @Override
    void finishFreezable(FinishContext context) {
        defaultSerializationDeclarations = context.getDefaultSerializationDeclarations();
    }

    @Override
    void verifyFreezable(VerifyContext context) { }
}
