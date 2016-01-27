package cloudkeeper.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that this annotation type element references a CloudKeeper element.
 *
 * <p>This annotation is a pure tagging annotation for annotation type elements with return type {@link String} or
 * {@code String[]}. The returned strings must be a fully-qualified name; otherwise, an error will occur at link time.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CloudKeeperElementReference { }
