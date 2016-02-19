package xyz.cloudkeeper.model.bare.element;

import java.util.List;

/**
 * CloudKeeper package.
 *
 * <p>This interface corresponds to {@link javax.lang.model.element.PackageElement} in the Java model, and both
 * interfaces can be implemented with covariant return types.
 */
public interface BarePackage extends BareAnnotatedConstruct, BareQualifiedNameable {
    /**
     * Human-readable name of the entity modeled by this interface.
     */
    String NAME = "package";

    /**
     * Returns a list of all plugin declarations in this package.
     *
     * @return list of all plugin declarations in this package, guaranteed non-null
     */
    List<? extends BarePluginDeclaration> getDeclarations();


    /**
     * Default implementations for standard methods.
     */
    final class Default {
        private Default() { }

        public static String toString(BarePackage instance) {
            return String.format(
                "%s '%s' (%d declarations)", NAME, instance.getQualifiedName(), instance.getDeclarations().size()
            );
        }
    }
}
