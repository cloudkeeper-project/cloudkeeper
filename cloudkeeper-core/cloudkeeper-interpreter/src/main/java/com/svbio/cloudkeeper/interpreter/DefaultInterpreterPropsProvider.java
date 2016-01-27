package com.svbio.cloudkeeper.interpreter;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import com.svbio.cloudkeeper.interpreter.DependencyGraph.HasValue;
import com.svbio.cloudkeeper.model.api.staging.StagingArea;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeCompositeModule;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeCompositeModuleDeclaration;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeInputModule;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeLoopModule;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeModule;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeModuleDeclarationVisitor;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeModuleVisitor;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeProxyModule;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeSimpleModuleDeclaration;
import com.svbio.cloudkeeper.model.runtime.execution.RuntimeExecutionTrace;
import com.svbio.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

enum DefaultInterpreterPropsProvider implements InterpreterPropsProvider {
    INSTANCE;

    private static void requireEmptyExecutionTraceOrModuleType(RuntimeExecutionTrace executionTrace) {
        if (!executionTrace.isEmpty() && executionTrace.getType() != RuntimeExecutionTrace.Type.MODULE) {
            throw new IllegalArgumentException(String.format(
                "Expected empty execution trace or execution trace with type %s, but got '%s'.",
                RuntimeExecutionTrace.Type.MODULE, executionTrace
            ));
        }
    }

    private static void requireLoopExecutionTrace(RuntimeExecutionTrace executionTrace) {
        if (
            !executionTrace.isEmpty()
            && !EnumSet.of(RuntimeExecutionTrace.Type.MODULE, RuntimeExecutionTrace.Type.ITERATION)
                .contains(executionTrace.getType())
        ) {
            throw new IllegalArgumentException(String.format(
                "Expected empty execution trace or execution trace with type in %s, but got '%s'.",
                EnumSet.of(RuntimeExecutionTrace.Type.MODULE, RuntimeExecutionTrace.Type.ITERATION), executionTrace
            ));
        }
    }

    @Override
    public Props provideInterpreterProps(
            final LocalInterpreterProperties interpreterProperties,
            final StagingArea stagingArea,
            final int moduleId,
            List<HasValue> inPortsHasValueList,
            BitSet recomputedInPorts,
            BitSet requestedOutPorts) {
        Objects.requireNonNull(interpreterProperties);
        RuntimeModule runtimeModule = stagingArea.getAnnotatedExecutionTrace().getModule();
        if (inPortsHasValueList.size() != runtimeModule.getInPorts().size()
                || inPortsHasValueList.contains(HasValue.PENDING_VALUE_CHECK)) {
            throw new IllegalArgumentException("Invalid list of in-ports that have a value.");
        } else if (recomputedInPorts.length() > runtimeModule.getInPorts().size()) {
            throw new IllegalArgumentException(String.format(
                "Invalid set of recomputed in-ports (with indices %s) specified for %s.",
                recomputedInPorts, runtimeModule
            ));
        } else if (requestedOutPorts.isEmpty() || requestedOutPorts.length() > runtimeModule.getOutPorts().size()) {
            throw new IllegalArgumentException(String.format(
                "Invalid set of requested out-ports (with indices %s) specified for %s.",
                requestedOutPorts, runtimeModule
            ));
        }

        ImmutableList<HasValue> localInPortsHasValueList = ImmutableList.copyOf(inPortsHasValueList);
        BitSet recomputedInPortsClone = (BitSet) recomputedInPorts.clone();
        BitSet requestedOutPortsClone = (BitSet) requestedOutPorts.clone();

        @Nullable Creator<UntypedActor> actorCreator = runtimeModule.accept(
            new RuntimeModuleVisitor<Creator<UntypedActor>, Void>() {
                @Override
                public Creator<UntypedActor> visit(RuntimeInputModule module, @Nullable Void ignored) {
                    requireEmptyExecutionTraceOrModuleType(stagingArea.getAnnotatedExecutionTrace());
                    return new InputModuleInterpreterActor.Factory(interpreterProperties, stagingArea, moduleId);
                }

                @Override
                public Creator<UntypedActor> visit(RuntimeCompositeModule compositeModule, @Nullable Void ignored) {
                    requireEmptyExecutionTraceOrModuleType(stagingArea.getAnnotatedExecutionTrace());
                    return new CompositeModuleInterpreterActor.Factory(interpreterProperties, stagingArea,
                        DefaultInterpreterPropsProvider.this, moduleId, localInPortsHasValueList, recomputedInPortsClone,
                        requestedOutPortsClone);
                }

                @Override
                public Creator<UntypedActor> visit(RuntimeLoopModule module, @Nullable Void ignored) {
                    requireLoopExecutionTrace(stagingArea.getAnnotatedExecutionTrace());
                    RuntimeExecutionTrace executionTrace = stagingArea.getAnnotatedExecutionTrace();
                    if (executionTrace.isEmpty() || executionTrace.getType() != RuntimeExecutionTrace.Type.ITERATION) {
                        return new LoopModuleInterpreterActor.Factory(interpreterProperties, stagingArea,
                            DefaultInterpreterPropsProvider.this, moduleId, recomputedInPortsClone, requestedOutPortsClone);
                    } else {
                        return new CompositeModuleInterpreterActor.Factory(interpreterProperties, stagingArea,
                            DefaultInterpreterPropsProvider.this, 0, localInPortsHasValueList, recomputedInPortsClone,
                            requestedOutPortsClone);
                    }
                }

                @Override
                public Creator<UntypedActor> visit(final RuntimeProxyModule module, @Nullable Void ignored) {
                    requireEmptyExecutionTraceOrModuleType(stagingArea.getAnnotatedExecutionTrace());
                    return module.getDeclaration().accept(
                        new RuntimeModuleDeclarationVisitor<Creator<UntypedActor>, Void>() {
                            @Override
                            public Creator<UntypedActor> visit(RuntimeCompositeModuleDeclaration declaration,
                                    @Nullable Void ignored2) {
                                return new CompositeModuleInterpreterActor.Factory(interpreterProperties, stagingArea,
                                    DefaultInterpreterPropsProvider.this, moduleId, localInPortsHasValueList,
                                    recomputedInPortsClone, requestedOutPortsClone);
                            }

                            @Override
                            public Creator<UntypedActor> visit(RuntimeSimpleModuleDeclaration declaration,
                                    @Nullable Void ignored2) {
                                return new SimpleModuleInterpreterActor.Factory(interpreterProperties, stagingArea,
                                    moduleId, recomputedInPortsClone, requestedOutPortsClone);
                            }
                        },
                        null
                    );
                }
            },
            null
        );
        assert actorCreator != null;
        return Props.create(actorCreator);
    }
}
