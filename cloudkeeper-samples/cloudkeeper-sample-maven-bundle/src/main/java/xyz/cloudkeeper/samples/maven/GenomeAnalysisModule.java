package xyz.cloudkeeper.samples.maven;

import xyz.cloudkeeper.dsl.CompositeModule;
import xyz.cloudkeeper.dsl.CompositeModulePlugin;
import xyz.cloudkeeper.dsl.InputModule;

@CompositeModulePlugin("Analyzes String consisting of DNA fragments")
public abstract class GenomeAnalysisModule extends CompositeModule<GenomeAnalysisModule> {
    public abstract InPort<String> reads();
    public abstract OutPort<String> report();

    private final InputModule<String> sequence = value("ACTG");

    private final AvgLineLengthModule avgLineLengthModule = child(AvgLineLengthModule.class)
        .text().from(reads());
    private final FindModule findModule = child(FindModule.class)
        .text().from(reads())
        .substring().from(sequence);
    private final ReportModule reportModule = child(ReportModule.class)
        .avgLineLength().from(avgLineLengthModule.avg())
        .subsequence().from(sequence)
        .wasDetected().from(findModule.wasFound());

    { report().from(reportModule.report()); }
}
