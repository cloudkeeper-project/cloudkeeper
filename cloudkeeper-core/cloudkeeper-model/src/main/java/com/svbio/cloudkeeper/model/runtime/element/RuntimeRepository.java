package com.svbio.cloudkeeper.model.runtime.element;

import com.svbio.cloudkeeper.model.Immutable;
import com.svbio.cloudkeeper.model.immutable.element.Name;
import com.svbio.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;

/**
 *
 */
public interface RuntimeRepository extends Immutable {
    /**
     * Human-readable name of the entity modeled by this interface.
     */
    String NAME = "repository";

    /**
     * Returns the bundles that constitute this repository.
     *
     * <p>Every repository implicitly contains the system bundle. The returned list only contains the explicit bundles,
     * and not the system bundle.
     *
     * @return the bundles that constitute this repository, except for the system bundle
     */
    ImmutableList<? extends RuntimeBundle> getBundles();

    /**
     * Returns the language element with the given type and name, or {@code null} if there is no such element.
     *
     * <p>This method returns the language element with the given name if it is contained in the system bundle or if it
     * is contained in any of the bundles returned by {@link #getBundles()}. If a language element with the given name
     * cannot be found, or if the type is not as specified, this method returns null.
     *
     * <p>Note that different bundles may provide package elements with the same name. In this case, the first package
     * element found will be returned. The search order consists of, first, the system bundle and then the bundles
     * returned by {@link #getBundles()} (in list order). Other language elements cannot share the same name.
     *
     * @param clazz class object representing the type of the requested language element
     * @param name name of the requested language element
     * @param <T> type of the requested language element
     * @return the language element with the given type and name, or {@code null} if there is no such element
     */
    @Nullable
    <T extends RuntimeElement> T getElement(Class<T> clazz, Name name);
}
