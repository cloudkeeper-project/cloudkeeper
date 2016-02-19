package xyz.cloudkeeper.model.immutable.element;

import xyz.cloudkeeper.model.immutable.ParseException;
import xyz.cloudkeeper.model.util.ImmutableList;

import javax.lang.model.SourceVersion;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Objects;

/**
 * CloudKeeper identifier.
 *
 * CloudKeeper identifiers are valid Java identifiers according to §3.8 of the Java Language Specification.
 */
@XmlJavaTypeAdapter(JAXBAdapters.SimpleNameAdapter.class)
public final class SimpleName extends Name {
    private static final long serialVersionUID = -1076020937669455050L;

    SimpleName(String identifier) {
        super(requireSimpleName(identifier));
    }

    /**
     * Creates and returns a new {@code Identifier} instance from the given string.
     *
     * @param identifier identifier string
     * @return new identifier instance
     * @throws NullPointerException if the given string is null
     * @throws ParseException if the given string is not a valid Java identifier according to §3.8 JLS
     */
    public static SimpleName identifier(String identifier) {
        return new SimpleName(Objects.requireNonNull(identifier));
    }

    /**
     * Returns whether the given string is a valid simple name.
     *
     * @throws NullPointerException if the given string is null
     */
    public static boolean isIdentifier(String identifier) {
        return SourceVersion.isIdentifier(identifier) && !SourceVersion.isKeyword(identifier);
    }

    private static String requireSimpleName(String string) {
        if (!isIdentifier(string)) {
            throw new ParseException(String.format(
                "Expected valid identifier according to §3.8 of the Java Language Specification, but got %s.",
                string
            ));
        }
        return string;
    }

    @Override
    public ImmutableList<SimpleName> asList() {
        return ImmutableList.of(this);
    }

    @Override
    public SimpleName subname(int fromIndex, int toIndex) {
        requireValidRange(1, fromIndex, toIndex);
        return this;
    }

    @Override
    public SimpleName toSimpleName() {
        return this;
    }
}
