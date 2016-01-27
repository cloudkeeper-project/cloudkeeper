package com.svbio.cloudkeeper.samples.maven;

import com.svbio.cloudkeeper.dsl.CompositeModule;
import com.svbio.cloudkeeper.dsl.CompositeModulePlugin;
import com.svbio.cloudkeeper.dsl.InputModule;

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
