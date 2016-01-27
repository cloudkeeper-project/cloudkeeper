package com.svbio.cloudkeeper.dsl;

/**
 * Internal interface that represents a connectable entity (for instance, a module port or an input module).
 */
interface Connectable {
    /**
     * Returns the {@link Module.Port} proxied by this instance.
     *
     * Proxy instances are characterized by being unequal to the result of their {@code getPort()} method.
     *
     * @return port (guaranteed to be non-null)
     */
    Module<?>.Port getPort();
}
