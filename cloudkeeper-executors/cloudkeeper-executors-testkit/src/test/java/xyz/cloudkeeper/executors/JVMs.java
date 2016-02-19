package xyz.cloudkeeper.executors;

import xyz.cloudkeeper.model.util.ImmutableList;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class JVMs {
    private JVMs() {
        throw new AssertionError(String.format("No %s instances for you!", getClass().getName()));
    }

    /**
     * Returns a classpath that for the given classes.
     *
     * <p>The returned classpath may be passed as {@code -classpath} argument when a new Java virtual machine is
     * started.
     *
     * @param classes classes that need to be available on the classpath
     * @return the classpath
     */
    static String classpathForClasses(List<Class<?>> classes) {
        Set<Path> paths = new LinkedHashSet<>();
        try {
            for (Class<?> clazz: classes) {
                paths.add(Paths.get(clazz.getProtectionDomain().getCodeSource().getLocation().toURI()));
            }
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Unexpected exception!", exception);
        }
        String pathSeparator = System.getProperty("path.separator");
        boolean first = true;
        StringBuilder stringBuilder = new StringBuilder(1024);
        for (Path path: paths) {
            if (first) {
                first = false;
            } else {
                stringBuilder.append(pathSeparator);
            }
            stringBuilder.append(path);
        }
        return stringBuilder.toString();
    }

    /**
     * Returns a command that can be passed to {@link java.lang.ProcessBuilder}.
     *
     * <p>The JVM arguments will always include {@code -enableassertions}.
     *
     * @param mainClass main Java class
     * @param classesNeeded Classes that need to be on the classpath. This must usually include {@code mainClass}.
     * @param additionalJVMArguments additional JVM command-line arguments
     * @return the command
     */
    static ImmutableList<String> command(Class<?> mainClass, List<Class<?>> classesNeeded,
            String... additionalJVMArguments) {
        Path javaPath = Paths.get(System.getProperty("java.home")).resolve("bin").resolve("java");
        String classpath = classpathForClasses(classesNeeded);

        List<String> command = new ArrayList<>();
        command.add(javaPath.toString());
        command.add("-enableassertions");
        command.add("-classpath");
        command.add(classpath);
        command.addAll(Arrays.asList(additionalJVMArguments));
        command.add(mainClass.getName());

        return ImmutableList.copyOf(command);
    }
}
