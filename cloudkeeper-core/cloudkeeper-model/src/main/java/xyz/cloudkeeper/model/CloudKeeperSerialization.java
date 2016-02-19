package xyz.cloudkeeper.model;

import xyz.cloudkeeper.model.api.Marshaler;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Indicates the serialization plug-in declarations to be used for values associated with the annotated element.
 *
 * <p>This annotation is the Java equivalent of {@link cloudkeeper.annotations.CloudKeeperSerialization}.
 *
 * @see cloudkeeper.annotations.CloudKeeperSerialization
 */
@ModelEquivalent(cloudkeeper.annotations.CloudKeeperSerialization.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface CloudKeeperSerialization {
    Class<? extends Marshaler<?>>[] value();
}
