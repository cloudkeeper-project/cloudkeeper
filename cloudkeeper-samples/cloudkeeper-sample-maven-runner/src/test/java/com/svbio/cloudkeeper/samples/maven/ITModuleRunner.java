package com.svbio.cloudkeeper.samples.maven;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Verifies that an execution specified by a Maven dependency, a module name, and in- and out-port names can be run
 * successfully.
 *
 * <p>This integration test requires that the assembly plugin has been run before, in order to create the Maven
 * repository.
 */
public class ITModuleRunner {
    private Path mavenRepositoryPath;

    @BeforeClass
    public void setup() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream("maven.properties")) {
            Properties properties = new Properties();
            properties.load(inputStream);
            mavenRepositoryPath = Paths.get(properties.getProperty("testrepo"));
        }
    }

    @Test
    public void test() throws Exception {
        List<String> reads = Arrays.asList("ACCTG", "CTAA", "GATCTTTACGAA", "CGAAT");
        Map<String, Object> configMap = new LinkedHashMap<>();
        configMap.put("com.svbio.cloudkeeper.samples.maven.local", mavenRepositoryPath.toString());
        configMap.put("com.svbio.cloudkeeper.samples.maven.offline", true);
        Config config = ConfigFactory.parseMap(configMap).withFallback(ConfigFactory.load());
        String input = reads.stream().collect(Collectors.joining(System.lineSeparator()));
        String output = ModuleRunner.runWithStringInput(config, input);

        Assert.assertTrue(output.contains(
            String.format("read length is %.2f,", reads.stream().collect(Collectors.averagingInt(String::length)))
        ));
    }
}
