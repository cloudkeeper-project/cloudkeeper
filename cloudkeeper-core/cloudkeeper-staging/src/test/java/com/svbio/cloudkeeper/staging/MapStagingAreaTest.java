package com.svbio.cloudkeeper.staging;

import com.svbio.cloudkeeper.contracts.StagingAreaContract;
import org.testng.annotations.Factory;

public class MapStagingAreaTest {
    @Factory
    public Object[] contractTests() {
        return new Object[] {
            new StagingAreaContract(
                (identifier, runtimeContext, executionTrace) -> new MapStagingArea(runtimeContext, executionTrace)
            )
        };
    }
}
