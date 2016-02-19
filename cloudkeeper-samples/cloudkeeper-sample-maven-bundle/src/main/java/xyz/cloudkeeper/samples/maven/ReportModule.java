package xyz.cloudkeeper.samples.maven;

import xyz.cloudkeeper.dsl.SimpleModule;
import xyz.cloudkeeper.dsl.SimpleModulePlugin;

@SimpleModulePlugin("Aggregates results into a report")
public abstract class ReportModule extends SimpleModule<ReportModule> {
    public abstract InPort<Double> avgLineLength();
    public abstract InPort<String> subsequence();
    public abstract InPort<Boolean> wasDetected();
    public abstract OutPort<String> report();

    @Override
    public void run() {
        report().set(String.format(
            "Report: Avg. read length is %.2f, and sequence '%s' was%s detected.",
            avgLineLength().get(), subsequence().get(), wasDetected().get() ? "" : " not"
        ));
    }
}
