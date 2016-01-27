package com.svbio.cloudkeeper.drm;

import org.testng.Assert;
import org.testng.annotations.Test;

public class DrmaaSimpleModuleExecutorTest {
    @Test
    public void testJobName() {
        Assert.assertEquals(DrmaaSimpleModuleExecutor.getJobName("öäü"), "oau");
        Assert.assertEquals(DrmaaSimpleModuleExecutor.getJobName("fr.ça.va"), "fr_ca_va");
    }
}
