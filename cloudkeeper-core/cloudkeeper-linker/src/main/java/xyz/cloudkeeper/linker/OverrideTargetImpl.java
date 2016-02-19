package xyz.cloudkeeper.linker;

import xyz.cloudkeeper.model.LinkerException;
import xyz.cloudkeeper.model.bare.execution.BareElementPatternTarget;
import xyz.cloudkeeper.model.bare.execution.BareElementTarget;
import xyz.cloudkeeper.model.bare.execution.BareExecutionTracePatternTarget;
import xyz.cloudkeeper.model.bare.execution.BareExecutionTraceTarget;
import xyz.cloudkeeper.model.bare.execution.BareOverrideTarget;
import xyz.cloudkeeper.model.bare.execution.BareOverrideTargetVisitor;
import xyz.cloudkeeper.model.immutable.execution.ExecutionTrace;
import xyz.cloudkeeper.model.runtime.execution.RuntimeElementPatternTarget;
import xyz.cloudkeeper.model.runtime.execution.RuntimeElementTarget;
import xyz.cloudkeeper.model.runtime.execution.RuntimeExecutionTracePatternTarget;
import xyz.cloudkeeper.model.runtime.execution.RuntimeExecutionTraceTarget;
import xyz.cloudkeeper.model.runtime.execution.RuntimeOverrideTarget;
import xyz.cloudkeeper.model.runtime.execution.RuntimeOverrideTargetVisitor;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.regex.Pattern;

abstract class OverrideTargetImpl extends LocatableImpl implements RuntimeOverrideTarget {
    private OverrideTargetImpl(BareOverrideTarget original, CopyContext parentContext) throws LinkerException {
        super(original, parentContext);
    }

    private enum CopyVisitor implements BareOverrideTargetVisitor<Try<? extends OverrideTargetImpl>, CopyContext> {
        INSTANCE;

        @Override
        public Try<ElementTargetImpl> visitElementTarget(BareElementTarget original,
                @Nullable CopyContext parentContext) {
            assert parentContext != null;
            return Try.run(() -> new ElementTargetImpl(original, parentContext));
        }

        @Override
        public Try<ElementPatternTargetImpl> visitElementPatternTarget(BareElementPatternTarget original,
                @Nullable CopyContext parentContext) {
            assert parentContext != null;
            return Try.run(() -> new ElementPatternTargetImpl(original, parentContext));
        }

        @Override
        public Try<ExecutionTraceTargetImpl> visitExecutionTraceTarget(BareExecutionTraceTarget original,
                @Nullable CopyContext parentContext) {
            assert parentContext != null;
            return Try.run(() -> new ExecutionTraceTargetImpl(original, parentContext));
        }

        @Override
        public Try<ElementExecutionTracePatternTargetImpl> visitExecutionTracePatternTarget(
                BareExecutionTracePatternTarget original, @Nullable CopyContext parentContext) {
            assert parentContext != null;
            return Try.run(() -> new ElementExecutionTracePatternTargetImpl(original, parentContext));
        }
    }

    static OverrideTargetImpl copyOf(BareOverrideTarget original, CopyContext parentContext) throws LinkerException {
        Preconditions.requireNonNull(original, parentContext);
        @Nullable Try<? extends OverrideTargetImpl> copyTry = original.accept(CopyVisitor.INSTANCE, parentContext);
        assert copyTry != null;
        return copyTry.get();
    }

    @Override
    final void collectEnclosed(Collection<AbstractFreezable> freezables) { }

    @Override
    final void preProcessFreezable(FinishContext context) { }

    @Override
    final void verifyFreezable(VerifyContext context) { }

    private static final class ElementTargetImpl extends OverrideTargetImpl implements RuntimeElementTarget {
        private final NameReference elementReference;
        private IElementImpl element;

        ElementTargetImpl(BareElementTarget original, CopyContext parentContext) throws LinkerException {
            super(original, parentContext);
            elementReference
                = new NameReference(original.getElement(), getCopyContext().newContextForProperty("element"));
        }

        @Override
        @Nullable
        public <T, P> T accept(BareOverrideTargetVisitor<T, P> visitor, @Nullable P parameter) {
            return visitor.visitElementTarget(this, parameter);
        }

        @Override
        @Nullable
        public <T, P> T accept(RuntimeOverrideTargetVisitor<T, P> visitor, @Nullable P parameter) {
            return visitor.visitElementTarget(this, parameter);
        }

        @Override
        public IElementImpl getElement() {
            require(State.FINISHED);
            return element;
        }

        @Override
        void finishFreezable(FinishContext context) throws LinkerException {
            element = context.resolveElement(elementReference);
        }
    }

    private static final class ElementPatternTargetImpl
            extends OverrideTargetImpl
            implements RuntimeElementPatternTarget {
        private final Pattern pattern;

        ElementPatternTargetImpl(BareElementPatternTarget original, CopyContext parentContext) throws LinkerException {
            super(original, parentContext);
            pattern = Preconditions.requireNonNull(
                original.getPattern(), getCopyContext().newContextForProperty("pattern"));
        }

        @Override
        @Nullable
        public <T, P> T accept(BareOverrideTargetVisitor<T, P> visitor, @Nullable P parameter) {
            return visitor.visitElementPatternTarget(this, parameter);
        }

        @Override
        @Nullable
        public <T, P> T accept(RuntimeOverrideTargetVisitor<T, P> visitor, @Nullable P parameter) {
            return visitor.visitElementPatternTarget(this, parameter);
        }

        @Override
        public Pattern getPattern() {
            return pattern;
        }

        @Override
        void finishFreezable(FinishContext context) { }
    }

    private static final class ExecutionTraceTargetImpl extends OverrideTargetImpl
            implements RuntimeExecutionTraceTarget {
        private final ExecutionTrace executionTrace;

        ExecutionTraceTargetImpl(BareExecutionTraceTarget original, CopyContext parentContext) throws LinkerException {
            super(original, parentContext);
            executionTrace = ExecutionTrace.copyOf(
                Preconditions.requireNonNull(
                    original.getExecutionTrace(), getCopyContext().newContextForProperty("executionTrace")
                )
            );
        }

        @Override
        @Nullable
        public <T, P> T accept(BareOverrideTargetVisitor<T, P> visitor, @Nullable P parameter) {
            return visitor.visitExecutionTraceTarget(this, parameter);
        }

        @Override
        @Nullable
        public <T, P> T accept(RuntimeOverrideTargetVisitor<T, P> visitor, @Nullable P parameter) {
            return visitor.visitExecutionTraceTarget(this, parameter);
        }

        @Override
        public ExecutionTrace getExecutionTrace() {
            return executionTrace;
        }

        @Override
        void finishFreezable(FinishContext context) { }
    }

    private static final class ElementExecutionTracePatternTargetImpl
            extends OverrideTargetImpl
            implements RuntimeExecutionTracePatternTarget {
        private final Pattern pattern;

        ElementExecutionTracePatternTargetImpl(BareExecutionTracePatternTarget original,
                CopyContext parentContext) throws LinkerException {
            super(original, parentContext);
            pattern = Preconditions.requireNonNull(
                original.getPattern(), getCopyContext().newContextForProperty("pattern"));
        }

        @Override
        @Nullable
        public <T, P> T accept(BareOverrideTargetVisitor<T, P> visitor, @Nullable P parameter) {
            return visitor.visitExecutionTracePatternTarget(this, parameter);
        }

        @Override
        @Nullable
        public <T, P> T accept(RuntimeOverrideTargetVisitor<T, P> visitor, @Nullable P parameter) {
            return visitor.visitExecutionTracePatternTarget(this, parameter);
        }

        @Override
        public Pattern getPattern() {
            return pattern;
        }

        @Override
        void finishFreezable(FinishContext context) { }
    }
}
