package xyz.cloudkeeper.dsl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface ExcludedSuperTypes {
    /**
     * List of super classes and interfaces that should be ignored for the CloudKeeper type hierarchy.
     */
    Class<?>[] value();
}
