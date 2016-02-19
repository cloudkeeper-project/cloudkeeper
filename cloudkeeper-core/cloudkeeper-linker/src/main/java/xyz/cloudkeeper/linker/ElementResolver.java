package xyz.cloudkeeper.linker;

import xyz.cloudkeeper.model.immutable.element.Name;
import xyz.cloudkeeper.model.immutable.element.SimpleName;
import xyz.cloudkeeper.model.runtime.element.RuntimeElement;

import javax.annotation.Nullable;
import java.util.Map;

interface ElementResolver {
    @Nullable
    <T extends RuntimeElement> T getElement(Class<T> clazz, Name name);

    /**
     * Default implementations for standard methods.
     */
    final class Default {
        private Default() { }

        /**
         * Default implementation for {@link ElementResolver#getElement}.
         */
        static <T extends RuntimeElement> T getElement(Class<T> clazz, Name name, Map<Name, PackageImpl> map) {
            Name packageName = name.getPackageName();
            @Nullable IElementImpl currentElement = map.get(packageName);
            if (currentElement == null) {
                return null;
            }
            for (SimpleName simpleName: name.subname(packageName.asList().size(), name.asList().size()).asList()) {
                currentElement = currentElement.getEnclosedElement(IElementImpl.class, simpleName);
                if (currentElement == null) {
                    return null;
                }
            }
            if (clazz.isInstance(currentElement)) {
                @SuppressWarnings("unchecked")
                T typedElement = (T) currentElement;
                return typedElement;
            } else {
                return null;
            }
        }
    }
}
