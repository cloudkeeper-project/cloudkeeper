package xyz.cloudkeeper.model.util;

import xyz.cloudkeeper.model.immutable.element.Version;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class BuildInformation {
    private static final String GENERATED_PROPERTIES = "build.properties";
    private static final String PROJECT_GROUP_ID_KEY = "project.groupId";
    private static final String PROJECT_ARTIFACT_ID_KEY = "project.artifactId";
    private static final String PROJECT_VERSION_KEY = "project.version";
    private static final String PROJECT_NAME_KEY = "project.name";

    public static final String PROJECT_GROUP_ID;
    public static final String PROJECT_ARTIFACT_ID;
    public static final Version PROJECT_VERSION;
    public static final String PROJECT_NAME;

    private BuildInformation() {
        throw new AssertionError(String.format("No %s instances for you!", getClass().getName()));
    }

    private static String getProperty(Properties properties, String key) {
        @Nullable String value = properties.getProperty(key);
        if (value == null) {
            throw new AssertionError(String.format("Expected property %s to be non-null. "
                + "This may indicated file corruption.", key));
        }
        return value;
    }

    static {
        try (InputStream inputStream = BuildInformation.class.getResourceAsStream(GENERATED_PROPERTIES)) {
            Properties properties = new Properties();
            properties.load(inputStream);
            PROJECT_GROUP_ID = getProperty(properties, PROJECT_GROUP_ID_KEY);
            PROJECT_ARTIFACT_ID = getProperty(properties, PROJECT_ARTIFACT_ID_KEY);
            PROJECT_VERSION = Version.valueOf(getProperty(properties, PROJECT_VERSION_KEY));
            PROJECT_NAME = getProperty(properties, PROJECT_NAME_KEY);
        } catch (IOException exception) {
            throw new AssertionError("Could not load required resource. This may indicate file corruption.", exception);
        }
    }
}
