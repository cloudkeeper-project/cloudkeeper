package com.svbio.cloudkeeper.model.immutable.element;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class VersionTest {
    @Test
    public void testNormalizeList() {
        Assert.assertEquals(Version.normalizedList(Arrays.asList(0)), Collections.singletonList(0));
        Assert.assertEquals(Version.normalizedList(Arrays.asList(0, 0)), Collections.singletonList(0));
        Assert.assertEquals(Version.normalizedList(Arrays.asList(23, 0, 4)), Arrays.asList(23, 0, 4));
        Assert.assertEquals(Version.normalizedList(Arrays.asList(23, 0, 0, 0)), Arrays.asList(23));
        Assert.assertEquals(Version.normalizedList(Arrays.asList(2, 1, 0)), Arrays.asList(2, 1));
    }

    @Test
    public void testParsing() {
        Version version;

        version = Version.valueOf("23");
        Assert.assertEquals(version.getNumbers(), Arrays.asList(23));
        Assert.assertTrue(version.getPrerelease().isEmpty());
        Assert.assertTrue(version.getBuild().isEmpty());
        Assert.assertTrue(
            version.compareTo(
                new Version(Arrays.asList(23, 0, 0, 0), Collections.emptyList(), Collections.emptyList())
            ) == 0
        );
        Assert.assertEquals(version.toNormalizedString(), "23.0.0");

        version = Version.valueOf("23.");
        Assert.assertTrue(version.getNumbers().isEmpty());
        Assert.assertTrue(version.getPrerelease().isEmpty());
        Assert.assertTrue(version.getBuild().isEmpty());
        try {
            version.toNormalizedString();
            Assert.fail();
        } catch (IllegalStateException exception) { }

        version = Version.valueOf("23.2");
        Assert.assertEquals(version.getNumbers(), Arrays.asList(23, 2));
        Assert.assertTrue(version.getPrerelease().isEmpty());
        Assert.assertTrue(version.getBuild().isEmpty());
        Assert.assertTrue(
            version.compareTo(
                new Version(Arrays.asList(23, 2, 0, 0), Collections.emptyList(), Collections.emptyList())
            ) == 0
        );
        Assert.assertEquals(version.toNormalizedString(), "23.2.0");

        version = Version.valueOf("23.2.1");
        Assert.assertEquals(version.getNumbers(), Arrays.asList(23, 2, 1));
        Assert.assertTrue(version.getPrerelease().isEmpty());
        Assert.assertTrue(version.getBuild().isEmpty());
        Assert.assertEquals(
            version,
            new Version(Arrays.asList(23, 2, 1), Collections.emptyList(), Collections.emptyList())
        );
        Assert.assertEquals(version.toNormalizedString(), "23.2.1");

        version = Version.valueOf("23.2.1.0");
        Assert.assertEquals(version.getNumbers(), Arrays.asList(23, 2, 1));
        Assert.assertTrue(version.getPrerelease().isEmpty());
        Assert.assertTrue(version.getBuild().isEmpty());
        Assert.assertTrue(
            version.compareTo(
                new Version(Arrays.asList(23, 2, 1, 0), Collections.emptyList(), Collections.emptyList())
            ) == 0
        );
        Assert.assertEquals(version.toNormalizedString(), "23.2.1");

        version = Version.valueOf("23.2.1.4-SNAPSHOT");
        Assert.assertEquals(version.getNumbers(), Arrays.asList(23, 2, 1, 4));
        Assert.assertEquals(version.getPrerelease(), Arrays.asList("SNAPSHOT"));
        Assert.assertTrue(version.getBuild().isEmpty());
        Assert.assertEquals(
            version,
            new Version(Arrays.asList(23, 2, 1, 4, 0), Arrays.<Object>asList("SNAPSHOT"), Collections.emptyList())
        );
        Assert.assertEquals(version.toNormalizedString(), version.toString());
        Assert.assertEquals(version.toString(), "23.2.1.4-SNAPSHOT");

        version = Version.valueOf("23.2.1.0+b.1");
        Assert.assertEquals(version.getNumbers(), Arrays.asList(23, 2, 1));
        Assert.assertTrue(version.getPrerelease().isEmpty());
        Assert.assertEquals(version.getBuild(), Arrays.asList("b", 1));
        Assert.assertTrue(
            version.compareTo(
                new Version(Arrays.asList(23, 2, 1), Collections.emptyList(), Arrays.<Object>asList("b", 1))
            ) == 0
        );
        Assert.assertEquals(version.toNormalizedString(), "23.2.1+b.1");
    }

    @Test
    public void testVersionComparison() {
        List<Version> versions = Arrays.asList(
            Version.valueOf("1-alpha"),
            Version.valueOf("1.0.0-alpha"),
            Version.valueOf("1.0.0-alpha+2"),
            Version.valueOf("1.0.0-alpha.1"),
            Version.valueOf("1.0.0-alpha.beta"),
            Version.valueOf("1.0.0-beta.2"),
            Version.valueOf("1-beta.3"),
            Version.valueOf("1.0.0-beta.11"),
            Version.valueOf("1.0.0"),
            Version.valueOf("1.0")
        );

        for (int index = 0; index < versions.size() - 1; ++index) {
            Assert.assertTrue(versions.get(index).compareTo(versions.get(index + 1)) <= 0);
        }
        for (Version version: versions) {
            Assert.assertTrue(version.isWellFormed());
            Assert.assertEquals(version, Version.valueOf(version.toString()));
        }

        Assert.assertEquals(Version.valueOf("1.0.0-alpha").compareTo(Version.valueOf("1.0.0-alpha+2")), 0);
    }

    @Test
    public void testInvalid() {
        Assert.assertTrue(Version.valueOf("foobar").compareTo(Version.valueOf("baz")) > 0);
        Assert.assertFalse(Version.valueOf("1.0.").isWellFormed());
        Assert.assertFalse(Version.valueOf("1.0.0b").isWellFormed());
        Assert.assertFalse(Version.valueOf("1.0.0-SNAPSHOT+").isWellFormed());
        Assert.assertFalse(Version.valueOf("1-SNAPSHOT+45..").isWellFormed());
        Assert.assertTrue(Version.valueOf("1-SNAPSHOT+45.d.g").isWellFormed());
    }
}
