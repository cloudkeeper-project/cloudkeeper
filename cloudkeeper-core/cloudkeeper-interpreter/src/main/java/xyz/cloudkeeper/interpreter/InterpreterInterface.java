package xyz.cloudkeeper.interpreter;

import javax.annotation.Nullable;
import java.util.Objects;

final class InterpreterInterface {
    private InterpreterInterface() {
        throw new AssertionError(String.format("No %s instances for you!", getClass().getName()));
    }

    /**
     * Tell parent interpreter that an out-port provides has a new signal.
     *
     * This class does not have to be serializable.
     */
    static final class SubmoduleOutPortHasSignal {
        private final int moduleId;
        private final int outPortId;

        SubmoduleOutPortHasSignal(int moduleId, int outPortId) {
            this.moduleId = moduleId;
            this.outPortId = outPortId;
        }

        @Override
        public boolean equals(@Nullable Object otherObject) {
            if (this == otherObject) {
                return true;
            } else if (otherObject == null || getClass() != otherObject.getClass()) {
                return false;
            }

            SubmoduleOutPortHasSignal other = (SubmoduleOutPortHasSignal) otherObject;
            return moduleId == other.moduleId
                && outPortId == other.outPortId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(moduleId, outPortId);
        }

        @Override
        public String toString() {
            return String.format("message out-port-has-signal (module-id %d, out-port-id %d)", moduleId, outPortId);
        }

        /**
         * Returns the relative ID of the module within the enclosing parent module.
         *
         * <p>Note that interpreters must not rely on calling
         * {@link xyz.cloudkeeper.model.runtime.element.module.RuntimeModule#getParent()}
         * on the {@link xyz.cloudkeeper.model.runtime.element.module.RuntimeModule} instance they were started
         * with. Therefore, the ID returned by this method is the relative ID that was given to the interpreter, and not
         * the relative ID returned by
         * {@link xyz.cloudkeeper.model.runtime.element.module.RuntimeModule#getIndex()}.
         *
         * @see xyz.cloudkeeper.model.runtime.element.module.RuntimeModule#getParent()
         * @see xyz.cloudkeeper.model.runtime.element.module.RuntimeModule#getIndex()
         */
        public int getModuleId() {
            return moduleId;
        }

        /**
         * Returns the index of the out-port within the the list of out-ports of the module.
         *
         * @see {@link xyz.cloudkeeper.model.runtime.element.module.RuntimeModule#getOutPorts()}.
         */
        public int getOutPortId() {
            return outPortId;
        }
    }

    /**
     * Tell child interpreter that an in-port has gotten a new signal.
     */
    static final class InPortHasSignal {
        private final int inPortId;

        InPortHasSignal(int inPortId) {
            this.inPortId = inPortId;
        }

        /**
         * Returns the index of the in-port within the the list of in-ports of the module.
         *
         * @see {@link xyz.cloudkeeper.model.runtime.element.module.RuntimeModule#getInPorts()}.
         */
        public int getInPortId() {
            return inPortId;
        }

        @Override
        public boolean equals(@Nullable Object otherObject) {
            return this == otherObject || (
                otherObject != null
                    && getClass() == otherObject.getClass()
                    && inPortId == ((InPortHasSignal) otherObject).inPortId
            );
        }

        @Override
        public int hashCode() {
            return inPortId;
        }

        @Override
        public String toString() {
            return String.format("message in-port-has-signal (in-port-id %d)", inPortId);
        }
    }
}
