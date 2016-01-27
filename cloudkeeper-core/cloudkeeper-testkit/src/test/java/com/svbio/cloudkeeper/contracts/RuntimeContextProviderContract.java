package com.svbio.cloudkeeper.contracts;

import com.svbio.cloudkeeper.model.api.RuntimeContextFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Contract test for basic bundle-loader functionality.
 *
 * <p>At present, this contract test only verifies that the expected exceptions are thrown in case of invalid arguments.
 */
public final class RuntimeContextProviderContract {
    private final Provider<RuntimeContextFactory> runtimeContextFactoryProvider;
    private RuntimeContextFactory runtimeContextFactory;

    public RuntimeContextProviderContract(Provider<RuntimeContextFactory> runtimeContextFactoryProvider) {
        this.runtimeContextFactoryProvider = runtimeContextFactoryProvider;
    }

    @BeforeClass
    public void setup() {
        runtimeContextFactoryProvider.preContract();

        runtimeContextFactory = runtimeContextFactoryProvider.get();
    }

    @Test
    public void createTestBadArguments() {
        try {
            runtimeContextFactory.newRuntimeContext(null);
            Assert.fail();
        } catch (NullPointerException ignored) { }
    }
}
