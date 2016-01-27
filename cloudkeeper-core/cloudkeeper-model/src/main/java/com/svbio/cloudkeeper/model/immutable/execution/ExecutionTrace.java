package com.svbio.cloudkeeper.model.immutable.execution;

import com.svbio.cloudkeeper.model.bare.execution.BareExecutionTrace;
import com.svbio.cloudkeeper.model.immutable.ParseException;
import com.svbio.cloudkeeper.model.immutable.element.Index;
import com.svbio.cloudkeeper.model.immutable.element.Key;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;
import com.svbio.cloudkeeper.model.runtime.execution.IllegalExecutionTraceException;
import com.svbio.cloudkeeper.model.runtime.execution.RuntimeExecutionTrace;
import com.svbio.cloudkeeper.model.runtime.execution.RuntimeExecutionTraceVisitor;
import com.svbio.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Immutable implementation of {@link BareExecutionTrace} interface.
 *
 * <p>This class is partially modeled after {@link java.nio.file.Path}.
 */
@XmlJavaTypeAdapter(JAXBAdapters.ExecutionTraceAdapter.class)
public abstract class ExecutionTrace implements RuntimeExecutionTrace, Serializable, Comparable<ExecutionTrace> {
    private static final long serialVersionUID = -5039104918790838563L;

    private static final char SEPARATOR = '/';
    private static final String IN_PORT_PREFIX = ":in:";
    private static final String OUT_PORT_PREFIX = ":out:";
    private static final char ARRAY_INDEX_SEPARATOR = ':';

    private static final Pattern ARRAY_INDICES_PATTERN = Pattern.compile("(?::[^:]+)*");
    private static final Pattern REFERENCE_PATTERN
        = Pattern.compile("(:in:|:out:)([^:]+)(" + ARRAY_INDICES_PATTERN + ')');
    private static final Pattern FRAME_WITH_OPTIONAL_REFERENCE_PATTERN
        = Pattern.compile("/?((?:[^/:]+/)*)(?:([^/:]+)(" + REFERENCE_PATTERN + ")?)?");

    private static final int FRAME_AND_CONTENT_OPTIONAL_REPEAT = 1;
    private static final int FRAME_BEFORE_REFERENCE = 2;
    private static final int REFERENCE = 3;

    private static final int IN_OR_OUT_PORT = 1;
    private static final int PORT_NAME = 2;
    private static final int ARRAY_INDICES = 3;

    /**
     * The regular expression represents valid strings according to the grammar specified by {@link BareExecutionTrace}.
     * The characters {@code C} and {@code #} represent the terminals {@code content} and {@code array-index}, character
     * {@code F} represents the non-terminal {@code frame}, and character {@code P} represents the non-terminal
     * {@code port}.
     */
    private static final String VALIDATE_REGEX = "C?(FC)*(F(P#*)?)?|P#*|#+";

    private enum RegexVisitor implements RuntimeExecutionTraceVisitor<Character, Void> {
        INSTANCE;

        @Override
        public Character visitContent(RuntimeExecutionTrace executionTrace, Void ignored) {
            return 'C';
        }

        @Override
        public Character visitModule(RuntimeExecutionTrace executionTrace, Void ignored) {
            return 'F';
        }

        @Override
        public Character visitIteration(RuntimeExecutionTrace executionTrace, Void ignored) {
            return 'F';
        }

        @Override
        public Character visitInPort(RuntimeExecutionTrace executionTrace, Void ignored) {
            return 'P';
        }

        @Override
        public Character visitOutPort(RuntimeExecutionTrace executionTrace, Void ignored) {
            return 'P';
        }

        @Override
        public Character visitArrayIndex(RuntimeExecutionTrace executionTrace, Void ignored) {
            return '#';
        }
    }

    private enum ResolveVisitor implements RuntimeExecutionTraceVisitor<ExecutionTrace, ExecutionTrace> {
        INSTANCE;

        @Override
        public ExecutionTrace visitModule(RuntimeExecutionTrace moduleElement, @Nullable ExecutionTrace oldTrace) {
            assert oldTrace != null;
            return oldTrace.resolveModule(moduleElement.getSimpleName());
        }

        @Override
        public ExecutionTrace visitContent(RuntimeExecutionTrace contentElement, @Nullable ExecutionTrace oldTrace) {
            assert oldTrace != null;
            return oldTrace.resolveContent();
        }

        @Override
        public ExecutionTrace visitIteration(RuntimeExecutionTrace iterationElement,
                @Nullable ExecutionTrace oldTrace) {
            assert oldTrace != null;
            return oldTrace.resolveIteration(iterationElement.getIndex());
        }

        @Override
        public ExecutionTrace visitInPort(RuntimeExecutionTrace inPortElement, @Nullable ExecutionTrace oldTrace) {
            assert oldTrace != null;
            return oldTrace.resolveInPort(inPortElement.getSimpleName());
        }

        @Override
        public ExecutionTrace visitOutPort(RuntimeExecutionTrace outPortElement, @Nullable ExecutionTrace oldTrace) {
            assert oldTrace != null;
            return oldTrace.resolveOutPort(outPortElement.getSimpleName());
        }

        @Override
        public ExecutionTrace visitArrayIndex(RuntimeExecutionTrace arrayIndexElement,
                @Nullable ExecutionTrace oldTrace) {
            assert oldTrace != null;
            return oldTrace.resolveArrayIndex(arrayIndexElement.getIndex());
        }
    }

    private ExecutionTrace() { }

    public static ExecutionTrace empty() {
        return EmptyExecutionTrace.INSTANCE;
    }

    /**
     * Returns an execution trace equivalent to the original bare execution trace.
     *
     * <p>If the original object is an instance of this class, the original object is simply returned (this is safe as
     * it is immutable). Otherwise, this method constructs a new execution trace by calling {@link #valueOf(String)}
     * on the result of {@link BareExecutionTrace#toString()}.
     *
     * @param original original bare execution trace
     * @return execution trace equivalent to the original bare execution trace
     */
    public static ExecutionTrace copyOf(BareExecutionTrace original) {
        if (original instanceof ExecutionTrace) {
            return (ExecutionTrace) original;
        }

        return valueOf(original.toString());
    }

    private static ExecutionTrace resolveFrame(ExecutionTrace trace, String tokenString, String fullString) {
        Key key = Key.valueOf(tokenString);
        if (key instanceof SimpleName) {
            return trace.resolveModule((SimpleName) key);
        } else if (key instanceof Index) {
            return trace.resolveIteration((Index) key);
        } else {
            throw new ParseException(String.format(
                "Cannot parse %s as execution trace because %s is neither simple name nor index.",
                fullString, tokenString
            ));
        }
    }

    private static ExecutionTrace resolveArrayIndices(ExecutionTrace appendTo, @Nullable String indices) {
        ExecutionTrace newTrace = appendTo;
        if (indices != null && !indices.isEmpty()) {
            for (String indexString: indices.substring(1).split(String.valueOf(ARRAY_INDEX_SEPARATOR))) {
                newTrace = newTrace.resolveArrayIndex(Index.index(indexString));
            }
        }
        return newTrace;
    }

    private static ExecutionTrace resolveReference(ExecutionTrace appendTo, @Nullable String inOrOutPort,
            @Nullable String portName, @Nullable String indices) {
        ExecutionTrace newTrace = appendTo;
        if (IN_PORT_PREFIX.equals(inOrOutPort)) {
            assert portName != null;
            newTrace = newTrace.resolveInPort(SimpleName.identifier(portName));
        } else if (OUT_PORT_PREFIX.equals(inOrOutPort)) {
            assert portName != null;
            newTrace = newTrace.resolveOutPort(SimpleName.identifier(portName));
        } else {
            assert indices == null;
            return newTrace;
        }

        return resolveArrayIndices(newTrace, indices);
    }

    private static ExecutionTrace parseFrameWithOptionalReference(String string, Matcher matcher) {
        ExecutionTrace newTrace = EmptyExecutionTrace.INSTANCE;

        if (string.startsWith(String.valueOf(SEPARATOR))) {
            newTrace = newTrace.resolveContent();
        }

        @Nullable String frameAndContentOptionalRepeat = matcher.group(FRAME_AND_CONTENT_OPTIONAL_REPEAT);
        assert frameAndContentOptionalRepeat != null : "Pattern of form '(X*)' should always be a match.";
        if (!frameAndContentOptionalRepeat.isEmpty()) {
            // Note that split() discards trailing empty strings
            for (String tokenString: frameAndContentOptionalRepeat.split(String.valueOf(SEPARATOR))) {
                newTrace = resolveFrame(newTrace, tokenString, string).resolveContent();
            }
        }

        @Nullable String frameBeforeReference = matcher.group(FRAME_BEFORE_REFERENCE);
        if (frameBeforeReference != null) {
            newTrace = resolveFrame(newTrace, frameBeforeReference, string);
        }

        if (matcher.group(REFERENCE) != null) {
            String inOrOutPort = matcher.group(REFERENCE + IN_OR_OUT_PORT);
            String portName = matcher.group(REFERENCE + PORT_NAME);
            @Nullable String indices = matcher.group(REFERENCE + ARRAY_INDICES);
            newTrace = resolveReference(newTrace, inOrOutPort, portName, indices);
        }

        return newTrace;
    }

    /**
     * Returns a new execution trace from the given string representation.
     *
     * @param string string representation of execution trace
     * @return execution trace
     * @throws ParseException if the given string does not conform to {@link BareExecutionTrace#toString()}
     */
    public static ExecutionTrace valueOf(String string) {
        Objects.requireNonNull(string);

        try {
            ExecutionTrace newTrace = EmptyExecutionTrace.INSTANCE;
            Matcher matcher = FRAME_WITH_OPTIONAL_REFERENCE_PATTERN.matcher(string);
            if (matcher.matches()) {
                return parseFrameWithOptionalReference(string, matcher);
            }

            Matcher referencePartMatcher = REFERENCE_PATTERN.matcher(string);
            if (referencePartMatcher.matches()) {
                return resolveReference(newTrace, referencePartMatcher.group(IN_OR_OUT_PORT),
                    referencePartMatcher.group(PORT_NAME), referencePartMatcher.group(ARRAY_INDICES));
            }

            if (ARRAY_INDICES_PATTERN.matcher(string).matches()) {
                return resolveArrayIndices(newTrace, string);
            }
        } catch (ParseException exception) {
            throw new ParseException(String.format("Cannot parse %s as execution trace.", string), exception);
        }

        throw new ParseException(String.format("Cannot parse %s as execution trace.", string));
    }

    @Override
    public abstract boolean equals(Object otherObject);

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();

    @Override
    public final <T, P> T accept(RuntimeExecutionTraceVisitor<T, P> visitor, @Nullable P parameter) {
        switch (getType()) {
            case CONTENT: return visitor.visitContent(this, parameter);
            case MODULE: return visitor.visitModule(this, parameter);
            case ITERATION: return visitor.visitIteration(this, parameter);
            case IN_PORT: return visitor.visitInPort(this, parameter);
            case OUT_PORT: return visitor.visitOutPort(this, parameter);
            case ARRAY_INDEX: return visitor.visitArrayIndex(this, parameter);
            default: throw new IllegalStateException("Unexpected element type in execution trace.");
        }
    }

    @Override
    public abstract ImmutableList<ExecutionTrace> asElementList();

    @Override
    public abstract Key getKey();

    @Override
    public abstract SimpleName getSimpleName();

    @Override
    public abstract Index getIndex();

    @Override
    public ExecutionTrace resolveExecutionTrace(RuntimeExecutionTrace trace) {
        @Nullable ExecutionTrace newTrace = this;
        for (RuntimeExecutionTrace traceElement: trace.asElementList()) {
            newTrace = traceElement.accept(ResolveVisitor.INSTANCE, newTrace);
            assert newTrace != null;
        }
        return newTrace;
    }

    @Override
    public abstract ExecutionTrace resolveModule(SimpleName moduleName);

    @Override
    public abstract ExecutionTrace resolveIteration(Index index);

    @Override
    public abstract ExecutionTrace resolveContent();

    @Override
    public abstract ExecutionTrace resolveInPort(SimpleName inPortName);

    @Override
    public abstract ExecutionTrace resolveOutPort(SimpleName outPortName);

    @Override
    public abstract ExecutionTrace resolveArrayIndex(Index index);

    @Override
    public abstract ExecutionTrace subtrace(int beginIndex, int endIndex);

    private static void requireValidSubtraceArguments(int beginIndex, int endIndex, int size) {
        if (beginIndex < 0) {
            throw new IndexOutOfBoundsException("fromIndex = " + beginIndex);
        } else if (endIndex > size) {
            throw new IndexOutOfBoundsException("toIndex = " + endIndex);
        } else if (beginIndex > endIndex) {
            throw new IllegalArgumentException(String.format("fromIndex(%d) > toIndex(%d)", beginIndex, endIndex));
        }
    }

    private static final class EmptyExecutionTrace extends ExecutionTrace {
        private static final long serialVersionUID = 8057904116861858087L;

        private static final ExecutionTrace INSTANCE = new EmptyExecutionTrace();

        @Override
        public boolean equals(Object otherObject) {
            return otherObject instanceof EmptyExecutionTrace;
        }

        @Override
        public int hashCode() {
            return ImmutableList.of().hashCode();
        }

        public String toString() {
            return "";
        }

        private Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }

        @Override
        public int compareTo(@Nullable ExecutionTrace other) {
            Objects.requireNonNull(other);
            return other instanceof EmptyExecutionTrace
                ? 0
                : -1;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public ImmutableList<ExecutionTrace> asElementList() {
            return ImmutableList.of();
        }

        @Override
        public final Type getType() {
            throw new IndexOutOfBoundsException("getType() called for empty execution trace.");
        }

        @Override
        public Key getKey() {
            throw new IndexOutOfBoundsException("getKey() called for empty execution trace.");
        }

        @Override
        public final SimpleName getSimpleName() {
            throw new IndexOutOfBoundsException("getSimpleName() called for empty execution trace.");
        }

        @Override
        public final Index getIndex() {
            throw new IndexOutOfBoundsException("getIndex() called for empty execution trace.");
        }

        @Override
        public final ExecutionTrace getFrames() {
            return this;
        }

        @Override
        public final ExecutionTrace getReference() {
            return this;
        }

        @Override
        public ExecutionTrace resolveContent() {
            return new Element(Type.CONTENT, null);
        }

        @Override
        public ExecutionTrace resolveModule(SimpleName moduleName) {
            return new Element(Type.MODULE, moduleName);
        }

        @Override
        public ExecutionTrace resolveIteration(Index index) {
            return new Element(Type.ITERATION, index);
        }

        @Override
        public ExecutionTrace resolveInPort(SimpleName inPortName) {
            return new Element(Type.IN_PORT, inPortName);
        }

        @Override
        public ExecutionTrace resolveOutPort(SimpleName outPortName) {
            return new Element(Type.OUT_PORT, outPortName);
        }

        @Override
        public ExecutionTrace resolveArrayIndex(Index index) {
            return new Element(Type.ARRAY_INDEX, index);
        }

        @Override
        public final ExecutionTrace subtrace(int beginIndex, int endIndex) {
            requireValidSubtraceArguments(beginIndex, endIndex, 0);
            return this;
        }
    }

    static ArrayList<ExecutionTrace> join(ExecutionTrace first, ExecutionTrace second) {
        final ArrayList<ExecutionTrace> elements = new ArrayList<>(2);
        elements.add(first);
        elements.add(second);
        return elements;
    }

    static ArrayList<ExecutionTrace> join(List<ExecutionTrace> list, ExecutionTrace element) {
        final ArrayList<ExecutionTrace> elements = new ArrayList<>(list.size() + 1);
        elements.addAll(list);
        elements.add(element);
        return elements;
    }

    private static final EnumSet<Type> CALL_STACK_TYPES = EnumSet.of(Type.CONTENT, Type.MODULE, Type.ITERATION);

    private static final EnumSet<Type> PORT_TYPES
        = EnumSet.of(Type.IN_PORT, Type.OUT_PORT);

    private static final class Element extends ExecutionTrace {
        private static final long serialVersionUID = 4292712979855641241L;

        private final Type type;
        @Nullable private final Key value;

        private Element(Type type, @Nullable Key value) {
            assert type == Type.CONTENT || value != null;

            this.type = type;
            this.value = value;
        }

        @Override
        public boolean equals(Object otherObject) {
            if (this == otherObject) {
                return true;
            } else if (!(otherObject instanceof Element)) {
                return false;
            }

            Element other = (Element) otherObject;
            return type == other.type
                && Objects.equals(value, other.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, value);
        }

        @Override
        public String toString() {
            switch (type) {
                case CONTENT: return String.valueOf(SEPARATOR);
                case MODULE: return getSimpleName().toString();
                case ITERATION: return getIndex().toString();
                case IN_PORT: return IN_PORT_PREFIX + getSimpleName();
                case OUT_PORT: return OUT_PORT_PREFIX + getSimpleName();
                case ARRAY_INDEX: return String.valueOf(ARRAY_INDEX_SEPARATOR) + getIndex();
                default: throw new IllegalStateException("Unexpected element type in execution trace.");
            }
        }

        @Override
        public int compareTo(@Nullable ExecutionTrace other) {
            Objects.requireNonNull(other);
            if (other instanceof EmptyExecutionTrace) {
                return 1;
            }

            ExecutionTrace otherFirstElement = other.asElementList().get(0);
            int result = type.ordinal() - otherFirstElement.getType().ordinal();
            if (result == 0) {
                assert type == otherFirstElement.getType();
                switch (type) {
                    case MODULE: case IN_PORT: case OUT_PORT:
                        result = getSimpleName().compareTo(otherFirstElement.getSimpleName()); break;
                    case ITERATION: case ARRAY_INDEX:
                        result = getIndex().compareTo(otherFirstElement.getIndex()); break;
                    case CONTENT:
                        result = 0; break;
                    default:
                        throw new IllegalStateException("Unexpected element type in execution trace.");
                }

                assert (other.size() > 1) == (other instanceof ExecutionTraceImpl);
                if (result == 0 && other instanceof ExecutionTraceImpl) {
                    result = -1;
                }
            }
            return result;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public ImmutableList<ExecutionTrace> asElementList() {
            return ImmutableList.<ExecutionTrace>of(this);
        }

        @Override
        public final Type getType() {
            return type;
        }

        @Override
        public final ExecutionTrace getFrames() {
            return CALL_STACK_TYPES.contains(type)
                ? this
                : EmptyExecutionTrace.INSTANCE;
        }

        @Override
        public ExecutionTrace getReference() {
            return PORT_TYPES.contains(type)
                ? this
                : EmptyExecutionTrace.INSTANCE;
        }

        @Override
        public ExecutionTrace subtrace(int beginIndex, int endIndex) {
            requireValidSubtraceArguments(beginIndex, endIndex, 1);
            return endIndex == 1
                ? this
                : EmptyExecutionTrace.INSTANCE;
        }

        @Override
        public Key getKey() {
            return value;
        }

        @Override
        public SimpleName getSimpleName() {
            return (SimpleName) value;
        }

        @Override
        public Index getIndex() {
            return (Index) value;
        }

        @Override
        public ExecutionTrace resolveContent() {
            return new ExecutionTraceImpl(join(this, new Element(Type.CONTENT, null)));
        }

        @Override
        public ExecutionTrace resolveModule(SimpleName moduleName) {
            return new ExecutionTraceImpl(join(this, new Element(Type.MODULE, moduleName)));
        }

        @Override
        public ExecutionTrace resolveIteration(Index index) {
            return new ExecutionTraceImpl(join(this, new Element(Type.ITERATION, index)));
        }

        @Override
        public ExecutionTrace resolveInPort(SimpleName inPortName) {
            return new ExecutionTraceImpl(join(this, new Element(Type.IN_PORT, inPortName)));
        }

        @Override
        public ExecutionTrace resolveOutPort(SimpleName outPortName) {
            return new ExecutionTraceImpl(join(this, new Element(Type.OUT_PORT, outPortName)));
        }

        @Override
        public ExecutionTrace resolveArrayIndex(Index index) {
            return new ExecutionTraceImpl(join(this, new Element(Type.ARRAY_INDEX, index)));
        }
    }

    private static final class ExecutionTraceImpl extends ExecutionTrace {
        private static final long serialVersionUID = -2112851616498618765L;
        private static final Pattern VALID_TRACE_PATTERN = Pattern.compile(VALIDATE_REGEX);

        /**
         * Expected length of each element in an execution trace (used inside {@link #toString()}).
         */
        private static final int EXPECTED_ELEMENT_LENGTH = 16;

        private final ImmutableList<ExecutionTrace> elements;

        private ExecutionTraceImpl(List<ExecutionTrace> elements) {
            this.elements = ImmutableList.copyOf(requireValid(elements));
        }

        @Override
        public boolean equals(Object otherObject) {
            if (this == otherObject) {
                return true;
            } else if (!(otherObject instanceof ExecutionTraceImpl)) {
                return false;
            }

            return elements.equals(((ExecutionTraceImpl) otherObject).elements);
        }

        @Override
        public int hashCode() {
            return elements.hashCode();
        }

        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder(size() * EXPECTED_ELEMENT_LENGTH);
            elements.forEach(stringBuilder::append);
            return stringBuilder.toString();
        }

        @Override
        public int compareTo(@Nullable ExecutionTrace other) {
            Objects.requireNonNull(other);
            Iterator<ExecutionTrace> otherIterator = other.asElementList().iterator();
            for (ExecutionTrace element: asElementList()) {
                if (!otherIterator.hasNext()) {
                    return 1;
                }
                ExecutionTrace otherElement = otherIterator.next();

                int result = element.compareTo(otherElement);
                if (result != 0) {
                    return result;
                }
            }
            if (otherIterator.hasNext()) {
                return -1;
            }

            return 0;
        }

        /**
         * Verify that the elements follows the required form.
         *
         * @param elements list of elements
         */
        private static <T extends List<? extends ExecutionTrace>> T requireValid(T elements) {
            if (elements.size() < 2) {
                throw new IllegalArgumentException("Expected at least two elements.");
            }

            StringBuilder stringBuilder = new StringBuilder(elements.size());
            for (ExecutionTrace element: elements) {
                stringBuilder.append(element.accept(RegexVisitor.INSTANCE, null));
            }
            String sequence = stringBuilder.toString();

            if (!VALID_TRACE_PATTERN.matcher(sequence).matches()) {
                throw new IllegalExecutionTraceException(String.format(
                    "Sequence '%s' does not match regular regular expression '%s'.", sequence, VALIDATE_REGEX
                ));
            }

            return elements;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public int size() {
            return elements.size();
        }

        @Override
        public ImmutableList<ExecutionTrace> asElementList() {
            return elements;
        }

        @Override
        public Type getType() {
            return elements.get(elements.size() - 1).getType();
        }

        @Override
        public Key getKey() {
            return elements.get(elements.size() - 1).getKey();
        }

        @Override
        public SimpleName getSimpleName() {
            return elements.get(elements.size() - 1).getSimpleName();
        }

        @Override
        public Index getIndex() {
            return elements.get(elements.size() - 1).getIndex();
        }

        @Override
        public ExecutionTrace getFrames() {
            int index = 0;
            for (ExecutionTrace element: elements) {
                if (!CALL_STACK_TYPES.contains(element.getType())) {
                    return subtrace(0, index);
                }
                ++index;
            }
            return this;
        }

        @Override
        public ExecutionTrace getReference() {
            int index = 0;
            for (ExecutionTrace element: elements) {
                if (PORT_TYPES.contains(element.getType())) {
                    return subtrace(index, elements.size());
                }
                ++index;
            }
            return EmptyExecutionTrace.INSTANCE;
        }

        @Override
        public ExecutionTrace subtrace(int beginIndex, int endIndex) {
            requireValidSubtraceArguments(beginIndex, endIndex, elements.size());

            int size = endIndex - beginIndex;
            if (size == 0) {
                return EmptyExecutionTrace.INSTANCE;
            } else if (size == 1) {
                return elements.get(beginIndex);
            } else if (size == elements.size()) {
                return this;
            } else {
                return new ExecutionTraceImpl(elements.subList(beginIndex, endIndex));
            }
        }

        @Override
        public ExecutionTrace resolveContent() {
            return new ExecutionTraceImpl(join(elements, new Element(Type.CONTENT, null)));
        }

        @Override
        public ExecutionTrace resolveModule(SimpleName moduleName) {
            return new ExecutionTraceImpl(join(elements, new Element(Type.MODULE, moduleName)));
        }

        @Override
        public ExecutionTrace resolveIteration(Index index) {
            return new ExecutionTraceImpl(join(elements, new Element(Type.ITERATION, index)));
        }

        @Override
        public ExecutionTrace resolveInPort(SimpleName inPortName) {
            return new ExecutionTraceImpl(join(elements, new Element(Type.IN_PORT, inPortName)));
        }

        @Override
        public ExecutionTrace resolveOutPort(SimpleName outPortName) {
            return new ExecutionTraceImpl(join(elements, new Element(Type.OUT_PORT, outPortName)));
        }

        @Override
        public ExecutionTrace resolveArrayIndex(Index index) {
            return new ExecutionTraceImpl(join(elements, new Element(Type.ARRAY_INDEX, index)));
        }
    }
}
