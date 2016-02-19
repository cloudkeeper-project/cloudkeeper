package xyz.cloudkeeper.staging;

import org.testng.annotations.Factory;
import xyz.cloudkeeper.contracts.StagingAreaContract;

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
