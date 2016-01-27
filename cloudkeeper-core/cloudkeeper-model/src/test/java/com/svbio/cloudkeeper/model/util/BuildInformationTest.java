package com.svbio.cloudkeeper.model.util;

import org.testng.Assert;
import org.testng.annotations.Test;

public class BuildInformationTest {
    @Test
    public void test() {
        Assert.assertTrue(BuildInformation.PROJECT_NAME.contains("CloudKeeper"));
        Assert.assertTrue(BuildInformation.PROJECT_VERSION.isWellFormed());
    }
}
