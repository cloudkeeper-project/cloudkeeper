package com.svbio.cloudkeeper.model.bare.element.module;

/**
 * Loop Module.
 *
 * Loop modules are composite modules that are meant to be executed multiple times, where (some of) the inputs of an
 * iteration are the outputs of the preceding iteration. An idiosyncrasy of loop modules therefore is that they
 * (typically) have ports that are both in- and out-ports (IO-ports in short). IO-ports share all the properties of both
 * in- and out-ports. That is, they need to have an incoming connection from a sibling of the loop module, and they can
 * also have outgoing connections to siblings of the loop module.
 * <p>
 * A loop module can also have regular out-ports. As for composite module, these ports do <strong>not</strong> have
 * incoming connections from sibling modules of the loop module. Therefore (unlike IO-ports), out-ports cannot have
 * outgoing connections to child modules (in order to pass data to the next iteration), because an out-port would not
 * have an initial value in the first iteration. When the loop module finishes, the output of an out-port will be the
 * value of this out-port in the last iteration.
 * <p>
 * Besides IO-ports, loop modules also have a special <strong>continue port</strong>. Technically, this port is a (pure)
 * out-port. It resides in the same namespace as all other ports, meaning that {@link #CONTINUE_PORT_NAME} cannot be
 * used for other ports of a loop modules.
 */
public interface BareLoopModule extends BareParentModule {
    /**
     * Human-readable name of the entity modeled by this interface.
     */
    String NAME = "loop module";

    /**
     * Name of the Boolean continue-port of a loop module.
     */
    String CONTINUE_PORT_NAME = "repeat";


    /**
     * Default implementations for standard methods.
     */
    final class Default {
        private Default() { }

        /**
         * Default implementation for {@link BareLoopModule#toString()}.
         */
        public static String toString(BareLoopModule instance) {
            return String.format("%s '%s'", NAME, instance.getSimpleName());
        }
    }
}
