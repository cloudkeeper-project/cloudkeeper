package xyz.cloudkeeper.model.runtime.element;

import xyz.cloudkeeper.model.bare.element.BareAnnotatedConstruct;
import xyz.cloudkeeper.model.bare.element.BareQualifiedNameable;
import xyz.cloudkeeper.model.immutable.element.Name;
import xyz.cloudkeeper.model.immutable.element.SimpleName;
import xyz.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents a CloudKeeper language element; that is, a declared entity in a repository.
 *
 * <p>This interface is similar to {@link javax.lang.model.element.Element}, which model program elements of the Java
 * language.
 */
public interface RuntimeElement extends BareQualifiedNameable, BareAnnotatedConstruct {
    @Override
    @Nonnull
    Name getQualifiedName();

    /**
     * Returns the innermost element within which this element is, loosely speaking, enclosed; or null if there is none.
     *
     * @see javax.lang.model.element.Element#getEnclosingElement()
     */
    @Nullable
    RuntimeElement getEnclosingElement();

    /**
     * Returns the elements that are, loosely speaking, directly enclosed by this element; or an empty list if none.
     *
     * @see javax.lang.model.element.Element#getEnclosedElements()
     */
    ImmutableList<? extends RuntimeElement> getEnclosedElements();

    /**
     * Returns the directly enclosed language element with the given type and name, or {@code null} if there is no such
     * element.
     *
     * <p>This method returns the directly enclosed language element with the given simple name. If such a language
     * element with the given name cannot be found, or if the type is not as specified, this method returns null.
     *
     * @param clazz class object representing the type of the requested language element
     * @param simpleName name of the requested language element
     * @param <T> type of the requested language element
     * @return the language element with the given type and name, or {@code null} if there is no such element
     */
    @Nullable
    <T extends RuntimeElement> T getEnclosedElement(Class<T> clazz, SimpleName simpleName);
}
