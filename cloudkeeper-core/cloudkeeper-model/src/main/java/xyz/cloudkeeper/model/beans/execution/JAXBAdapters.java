package xyz.cloudkeeper.model.beans.execution;

import xyz.cloudkeeper.model.bare.execution.BareElementPatternTarget;
import xyz.cloudkeeper.model.bare.execution.BareElementTarget;
import xyz.cloudkeeper.model.bare.execution.BareExecutionTracePatternTarget;
import xyz.cloudkeeper.model.bare.execution.BareExecutionTraceTarget;
import xyz.cloudkeeper.model.bare.execution.BareOverrideTargetVisitor;
import xyz.cloudkeeper.model.beans.element.MutableQualifiedNamable;
import xyz.cloudkeeper.model.immutable.execution.ExecutionTrace;
import xyz.cloudkeeper.model.util.XmlToStringAdapter;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.regex.Pattern;

final class JAXBAdapters {
    private JAXBAdapters() {
        throw new AssertionError(String.format("No %s instances for you!", getClass().getName()));
    }

    static final class PatternAdapter extends XmlToStringAdapter<Pattern> {
        @Override
        protected Pattern fromString(String original) {
            return Pattern.compile(original);
        }
    }

    private interface JAXBOverrideTarget {
        MutableOverrideTarget<?> toMutableOverrideTarget();
    }

    static final class JAXBElementPatternTarget implements JAXBOverrideTarget {
        @XmlValue
        @Nullable
        private Pattern pattern;

        @Override
        public MutableElementPatternTarget toMutableOverrideTarget() {
            return new MutableElementPatternTarget().setPattern(pattern);
        }
    }

    static final class JAXBElementTarget implements JAXBOverrideTarget {
        @XmlValue
        @Nullable
        private MutableQualifiedNamable element;

        @Override
        public MutableElementTarget toMutableOverrideTarget() {
            return new MutableElementTarget().setElement(element);
        }
    }

    static final class JAXBExecutionTracePatternTarget implements JAXBOverrideTarget {
        @XmlValue
        @Nullable
        private Pattern pattern;

        @Override
        public MutableExecutionTracePatternTarget toMutableOverrideTarget() {
            return new MutableExecutionTracePatternTarget().setPattern(pattern);
        }
    }

    static final class JAXBExecutionTraceTarget implements JAXBOverrideTarget {
        @XmlValue
        @Nullable
        private ExecutionTrace executionTrace;

        @Override
        public MutableExecutionTraceTarget toMutableOverrideTarget() {
            return new MutableExecutionTraceTarget().setExecutionTrace(executionTrace);
        }
    }

    private enum ToJAXBOverrideTargetVisitor implements BareOverrideTargetVisitor<JAXBOverrideTarget, Void> {
        INSTANCE;

        @Override
        public JAXBElementTarget visitElementTarget(BareElementTarget overrideTarget, @Nullable Void ignored) {
            JAXBElementTarget jaxbOverrideTarget = new JAXBElementTarget();
            jaxbOverrideTarget.element = (MutableQualifiedNamable) overrideTarget.getElement();
            return jaxbOverrideTarget;
        }

        @Override
        public JAXBElementPatternTarget visitElementPatternTarget(BareElementPatternTarget overrideTarget,
                @Nullable Void ignored) {
            JAXBElementPatternTarget jaxbOverrideTarget = new JAXBElementPatternTarget();
            jaxbOverrideTarget.pattern = overrideTarget.getPattern();
            return jaxbOverrideTarget;
        }

        @Override
        public JAXBExecutionTraceTarget visitExecutionTraceTarget(BareExecutionTraceTarget overrideTarget,
                @Nullable Void ignored) {
            JAXBExecutionTraceTarget jaxbOverrideTarget = new JAXBExecutionTraceTarget();
            jaxbOverrideTarget.executionTrace = (ExecutionTrace) overrideTarget.getExecutionTrace();
            return jaxbOverrideTarget;
        }

        @Override
        public JAXBExecutionTracePatternTarget visitExecutionTracePatternTarget(
                BareExecutionTracePatternTarget overrideTarget, @Nullable Void ignored) {
            JAXBExecutionTracePatternTarget jaxbOverrideTarget = new JAXBExecutionTracePatternTarget();
            jaxbOverrideTarget.pattern = overrideTarget.getPattern();
            return jaxbOverrideTarget;
        }
    }

    static final class MutableOverrideTargetAdapter extends XmlAdapter<Object, MutableOverrideTarget<?>> {
        @Override
        @Nullable
        public MutableOverrideTarget<?> unmarshal(@Nullable Object original) {
            @Nullable JAXBOverrideTarget jaxbOverrideTarget = (JAXBOverrideTarget) original;
            return jaxbOverrideTarget == null
                ? null
                : jaxbOverrideTarget.toMutableOverrideTarget();
        }

        @Override
        @Nullable
        public Object marshal(@Nullable MutableOverrideTarget<?> original) {
            return original == null
                ? null
                : original.accept(ToJAXBOverrideTargetVisitor.INSTANCE, null);
        }
    }
}
