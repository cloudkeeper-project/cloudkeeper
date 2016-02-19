package xyz.cloudkeeper.dsl;

import java.util.Collection;

/**
 * Proxy for {@link FromConnectable} of type {@code T} that can be connected to a {@link ToConnectable} of type
 * {@link Collection}{@code <U>}, provided that {@code T} extends {@code U}.
 *
 * This proxy is used to facilitate combine-into-array connections, that are necessary to gather the results of a module
 * that has an incoming apply-to-all connection.
 *
 * @param <T> element type
 */
final class CombineIntoArrayPort<T> implements FromConnectable<Collection<T>> {
    private final FromConnectable<? extends T> elementPort;

    CombineIntoArrayPort(FromConnectable<? extends T> elementPort) {
        this.elementPort = elementPort;
    }

    /**
     * Returns the {@link FromConnectable} proxied by this instance.
     */
    FromConnectable<? extends T> getProxiedFromConnectable() {
        return elementPort;
    }

    @Override
    public Module<?>.Port getPort() {
        return elementPort.getPort();
    }
}
