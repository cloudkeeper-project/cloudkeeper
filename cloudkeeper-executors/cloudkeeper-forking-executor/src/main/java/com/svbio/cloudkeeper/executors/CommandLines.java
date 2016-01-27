package com.svbio.cloudkeeper.executors;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Static utility methods for dealing with command lines.
 */
public final class CommandLines {
    private static final Pattern SINGLE_WHITESPACE_PATTERN = Pattern.compile("(\\s)");

    private CommandLines() {
        throw new AssertionError(String.format("No %s instances for you!", getClass().getName()));
    }

    /**
     * Returns the command-line string corresponding to the given list of strings.
     *
     * <p>This method returns a space-separated concatenation of all list elements, where each whitespace character in
     * each element has been end escaped with a backslash. No other escaping (other than for whitespace characters) is
     * performed by this method.
     *
     * @param commandLine command line
     * @return the command-line string
     */
    public static String escape(List<String> commandLine) {
        StringBuilder stringBuilder = new StringBuilder(1024);
        boolean first = true;
        for (String element: commandLine) {
            if (first) {
                first = false;
            } else {
                stringBuilder.append(' ');
            }
            stringBuilder.append(SINGLE_WHITESPACE_PATTERN.matcher(element).replaceAll("\\\\$1"));
        }
        return stringBuilder.toString();
    }
}
