package xyz.cloudkeeper.model.immutable.element;

import xyz.cloudkeeper.model.Immutable;
import xyz.cloudkeeper.model.util.ImmutableList;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Version class as defined by Semantic Versioning 2.0.0.
 *
 * <p>If the {@link String} passed to {@link #valueOf(String)} does not fit the semantic versioning standard as
 * described here, then versions will be compared as strings.
 *
 * <p>Note: this class has a natural ordering that is inconsistent with equals. As mandated by §10 ("Build metadata
 * SHOULD be ignored when determining version precedence"), the build metadata, that can be obtained via
 * {@link #getBuild()}, is disregarded by {@link #compareTo(Version)}. Likewise, the original version {@link String},
 * that can be obtained via {@link #toString()}, may be different even though {@link #compareTo(Version)} returns 0.
 *
 * <p>Instances of this class are immutable.
 *
 * @see <a href="http://semver.org">http://semver.org</a>
 * @see <a href="http://books.sonatype.com/mvnref-book/reference/pom-relationships-sect-pom-syntax.html">Maven: The
 *     Complete Reference, Section 3.3.1</a>
 */
@XmlJavaTypeAdapter(JAXBAdapters.VersionAdapter.class)
public final class Version implements Immutable, Comparable<Version>, Serializable {
    private static final long serialVersionUID = 9398167807745194L;

    private static final Version LATEST = new Version(Collections.singletonList(Integer.MAX_VALUE),
        Collections.emptyList(), Collections.emptyList(), "LATEST"
    );
    private static final Pattern VERSION_PATTERN = Pattern.compile(
        "(?<numbers>[0-9.]+)(-(?<prerelease>[0-9A-Za-z-.]+))?(\\+(?<build>[0-9A-Za-z-.]+))?"
    );

    private static final Pattern NUMBER_SEQUENCE_PATTERN = Pattern.compile(
        "([0-9]{1,9})(?:\\.([0-9]{1,9}))*"
    );
    private static final Pattern IDENTIFIER_SEQUENCE_PATTERN = Pattern.compile(
        "([0-9A-Za-z-]+)(?:\\.([0-9A-Za-z-]+))*"
    );
    private static final Pattern DOT_PATTERN = Pattern.compile("\\.");
    private static final int MAX_VERSION_NUMBER = 999999999;

    private final ImmutableList<Integer> numbers;
    private final ImmutableList<Object> prerelease;
    private final ImmutableList<Object> build;
    private final String original;

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        Version other = (Version) otherObject;
        return compareTo(other) == 0 && build.equals(other.build) && original.equals(other.original);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numbers, prerelease, build, original);
    }

    private Object writeReplace() {
        return new SerializableVersion(original);
    }

    private static final class SerializableVersion implements Serializable {
        private static final long serialVersionUID = -234593427236593511L;

        private final String original;

        private SerializableVersion(String original) {
            this.original = original;
        }

        private Object readResolve() {
            return valueOf(original);
        }
    }

    /**
     * Internal constructor that relies on being passed immutable objects.
     */
    private Version(List<Integer> numbers, List<?> prerelease, List<?> build, String original) {
        this.numbers = ImmutableList.copyOf(numbers);
        this.prerelease = ImmutableList.copyOf(prerelease);
        this.build = ImmutableList.copyOf(build);
        this.original = original == null
            ? toNormalizedString()
            : original;
    }

    public Version(List<Integer> numbers, List<?> prerelease, List<?> build) {
        this(
            normalizedList(Objects.requireNonNull(numbers)),
            Objects.requireNonNull(prerelease),
            verifyValidIdentifierList(Objects.requireNonNull(build)),
            null
        );
        for (Integer number: this.numbers) {
            if (number < 0 || number > MAX_VERSION_NUMBER) {
                // Anything else would not match our regular expression!
                throw new IllegalArgumentException(String.format(
                    "Expected numbers between 0 and %d, but got %s.", MAX_VERSION_NUMBER, numbers
                ));
            }
        }
    }

    public static Version latest() {
        return LATEST;
    }

    private static boolean isDigitSequence(CharSequence sequence) {
        int length = sequence.length();
        for (int index = 0; index < length; ++index) {
            // ASCII: '0' == 48 && '9' == 57
            // Unicode coincide in this range
            char character = sequence.charAt(index);
            if (character < '0' || character > '9') {
                return false;
            }
        }
        return true;
    }

    private static List<Integer> parseIntoImmutableNumberList(String numberListStr) {
        Matcher matcher = NUMBER_SEQUENCE_PATTERN.matcher(numberListStr);
        if (!matcher.matches()) {
            return null;
        }

        List<Integer> list = new ArrayList<>();
        for (String number: DOT_PATTERN.split(numberListStr)) {
            list.add(Integer.valueOf(number));
        }
        return normalizedList(list);
    }

    /**
     * Returns an immutable list of identifiers.
     *
     * @return list of identifiers, this list is empty if {@code identifierListStr} is null. If
     *     {@code identifierListStr} cannot be parsed, {@code null} is returned.
     */
    private static List<Object> parseIntoImmutableIdentifierList(String identifierListStr) {
        if (identifierListStr == null) {
            return Collections.emptyList();
        }

        Matcher prereleaseMatcher = IDENTIFIER_SEQUENCE_PATTERN.matcher(identifierListStr);
        if (!prereleaseMatcher.matches()) {
            return null;
        }

        List<Object> list = new ArrayList<>();
        for (String identifier: DOT_PATTERN.split(identifierListStr)) {
            list.add(
                isDigitSequence(identifier)
                    ? Integer.valueOf(identifier)
                    : identifier
            );
        }
        return Collections.unmodifiableList(list);
    }

    private static List<?> verifyValidIdentifierList(List<?> list) {
        for (Object identifier: list) {
            if (!(identifier instanceof Integer) && !(identifier instanceof String)) {
                throw new IllegalArgumentException(String.format(
                    "Expected list of Integer and String objects, but got %s", list
                ));
            }
        }
        return list;
    }

    public static Version valueOf(String versionString) {
        Matcher versionMatcher = VERSION_PATTERN.matcher(versionString);
        if (versionMatcher.matches()) {
            List<Integer> numbers = parseIntoImmutableNumberList(versionMatcher.group("numbers"));
            List<Object> prerelease = parseIntoImmutableIdentifierList(versionMatcher.group("prerelease"));
            List<Object> build = parseIntoImmutableIdentifierList(versionMatcher.group("build"));

            if (numbers != null && prerelease != null && build != null) {
                return new Version(
                    numbers,
                    prerelease,
                    build,
                    versionString
                );
            }
        } else if (versionString.equals(LATEST.original)) {
            return LATEST;
        }

        return new Version(Collections.<Integer>emptyList(), Collections.emptyList(), Collections.emptyList(),
            versionString);
    }

    /**
     * Compares two lists that contain only {@link Integer} and {@link String} elements.
     */
    private static int compareLists(List<?> left, List<?> right) {
        // §11. When major, minor, and patch are equal, a pre-release version has lower precedence than a normal
        // version. Example: 1.0.0-alpha < 1.0.0.
        int comparison = (left.isEmpty() ? 1 : 0) - (right.isEmpty() ? 1 : 0);
        if (comparison != 0) {
            return comparison;
        }

        Iterator<?> rightIterator = right.iterator();
        for (Object leftIdentifier: left) {
            if (!rightIterator.hasNext()) {
                // §11. A larger set of pre-release fields has a higher precedence than a smaller set, if all of the
                // preceding identifiers are equal.
                return 1;
            }
            Object rightIdentifier = rightIterator.next();

            if (leftIdentifier instanceof Integer) {
                if (rightIdentifier instanceof Integer) {
                    comparison = (Integer) leftIdentifier - (Integer) rightIdentifier;
                } else {
                    // §11. Numeric identifiers always have lower precedence than non-numeric identifiers.
                    comparison = -1;
                }
            } else {
                if (rightIdentifier instanceof Integer) {
                    comparison = 1;
                } else {
                    comparison = ((String) leftIdentifier).compareTo((String) rightIdentifier);
                }
            }
            if (comparison != 0) {
                return comparison;
            }
        }
        // See above, "a larger set ..."
        return rightIterator.hasNext()
            ? -1
            : 0;
    }

    static List<Integer> normalizedList(List<Integer> list) {
        ListIterator<Integer> iterator = list.listIterator(list.size());
        int lastNonZeroIndex = -1;
        while (iterator.hasPrevious()) {
            if (iterator.previous() != 0) {
                lastNonZeroIndex = iterator.nextIndex();
                break;
            }
        }
        return lastNonZeroIndex == -1
            ? ImmutableList.of(0)
            : list.subList(0, lastNonZeroIndex + 1);
    }

    @Override
    public int compareTo(Version other) {
        int wellFormedComparison = (isWellFormed() ? 1 : 0) - (other.isWellFormed() ? 1 : 0);
        if (wellFormedComparison != 0) {
            return wellFormedComparison;
        }

        if (numbers.isEmpty()) {
            // Since wellFormedComparison == 0, it must hold that other.numbers.isEmpty()!
            assert other.numbers.isEmpty();

            // Use ASCII sort order if the versions are not well-formed. This is also what Maven does in this case.
            return original.compareTo(other.original);
        }

        // numbers are always normalized, so no need to redo it here.
        int listComparison = compareLists(numbers, other.numbers);
        if (listComparison != 0) {
            return listComparison;
        }
        return compareLists(prerelease, other.prerelease);
        // §10. Build metadata SHOULD be ignored when determining version precedence.
    }

    /**
     * Returns the version numbers.
     *
     * @return list of version numbers. If the version is well-formed, the list is guaranteed to be non-empty.
     */
    public ImmutableList<Integer> getNumbers() {
        return numbers;
    }

    /**
     * Returns the pre-release version.
     *
     * @return list of identifiers, which are either {@link Integer} or {@link String} objects. {@link String} objects
     *     are guaranteed to contain at least one non-digit character.
     */
    public ImmutableList<Object> getPrerelease() {
        return prerelease;
    }

    /**
     * Returns the build metadata.
     *
     * @return list of identifiers, which are either {@link Integer} or {@link String} objects. {@link String} objects
     *     are guaranteed to contain at least one non-digit character.
     */
    public ImmutableList<Object> getBuild() {
        return build;
    }

    public boolean isWellFormed() {
        return !numbers.isEmpty();
    }

    @Override
    public String toString() {
        return original;
    }

    private static void appendNonEmptyIterable(StringBuilder stringBuilder, Iterable<?> objects) {
        Iterator<?> iterator = objects.iterator();

        assert iterator.hasNext();
        stringBuilder.append(iterator.next());
        while (iterator.hasNext()) {
            stringBuilder.append('.').append(iterator.next());
        }
    }

    /**
     * Returns the normalized {@link String} representation of this version object.
     *
     * @return normalized representation
     * @throws IllegalStateException if the version is not well-formed, that is, if {@link #isWellFormed()} returns
     *     false
     */
    public String toNormalizedString() {
        // We need to deal with the special cases first
        if (this == LATEST) {
            return LATEST.original;
        } else if (!isWellFormed()) {
            throw new IllegalStateException(String.format(
                "toNormalizedString() called on version %s that is not well-formed.", this
            ));
        }

        StringBuilder stringBuilder = new StringBuilder(32);
        appendNonEmptyIterable(stringBuilder, numbers);
        for (int count = numbers.size(); count < 3; ++count) {
            stringBuilder.append(".0");
        }
        if (!prerelease.isEmpty()) {
            stringBuilder.append('-');
            appendNonEmptyIterable(stringBuilder, prerelease);
        }
        if (!build.isEmpty()) {
            stringBuilder.append('+');
            appendNonEmptyIterable(stringBuilder, build);
        }
        return stringBuilder.toString();
    }
}
