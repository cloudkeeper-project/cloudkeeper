package xyz.cloudkeeper.model.immutable.execution;

import xyz.cloudkeeper.model.util.XmlToStringAdapter;

final class JAXBAdapters {
    private JAXBAdapters() {
        throw new AssertionError(String.format("No %s instances for you!", getClass().getName()));
    }

    static final class ExecutionTraceAdapter extends XmlToStringAdapter<ExecutionTrace> {
        @Override
        protected ExecutionTrace fromString(String original) {
            return ExecutionTrace.valueOf(original);
        }
    }
}
