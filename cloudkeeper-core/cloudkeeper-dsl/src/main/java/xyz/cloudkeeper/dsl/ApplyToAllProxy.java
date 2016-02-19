package xyz.cloudkeeper.dsl;

import java.util.Collection;

/**
 * Proxy for {@link FromConnectable} of type {@link Collection}{@code <T>} that can be connected to a
 * {@link ToConnectable} of type {@code U}, provided that {@code T} extends {@code U}.
 *
 * This proxy is used to facilitate apply-to-all connections.
 *
 * @param <T> element type
 */
final class ApplyToAllProxy<T> implements FromConnectable<T> {
    private final FromConnectable<? extends Collection<T>> arrayPort;

    ApplyToAllProxy(FromConnectable<? extends Collection<T>> arrayPort) {
        this.arrayPort = arrayPort;
    }

    /**
     * Returns the {@link FromConnectable} proxied by this instance.
     */
    FromConnectable<? extends Collection<T>> getProxiedFromConnectable() {
        return arrayPort;
    }

    @Override
    public Module<?>.Port getPort() {
        return arrayPort.getPort();
    }
}
