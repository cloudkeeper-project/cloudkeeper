package cloudkeeper.annotations;

import com.svbio.cloudkeeper.model.api.Marshaler;

import java.lang.annotation.Target;

/**
 * Indicates the serialization plug-in declarations to be used for values associated with the annotated element.
 *
 * <p>This annotation contains a list of names of {@link Marshaler} subclasses that
 * should be used for serializing values of the annotated type declaration (if used on a type declaration) or values
 * going through the annotated port (if used on a port of a module). The list defines the order according to which the
 * first <em>capable</em> {@link Marshaler} will be chosen.
 *
 * <p>This annotation is a pure CloudKeeper annotation, which can only be used for retrieving annotations using
 * {@link com.svbio.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace#getAnnotation(Class)}. This
 * annotation cannot, however, be added to a Java language element within the Java source code. The
 * {@link com.svbio.cloudkeeper.model.CloudKeeperSerialization} annotation must be used instead.
 */
@Target({ })
public @interface CloudKeeperSerialization {
    @CloudKeeperElementReference
    String[] value();
}
