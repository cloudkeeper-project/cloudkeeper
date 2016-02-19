package xyz.cloudkeeper.samples.maven;

import xyz.cloudkeeper.dsl.SimpleModule;
import xyz.cloudkeeper.dsl.SimpleModulePlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.stream.Collectors;

@SimpleModulePlugin("Computes the average line length in a text")
public abstract class AvgLineLengthModule extends SimpleModule<AvgLineLengthModule> {
    public abstract InPort<String> text();
    public abstract OutPort<Double> avg();

    @Override
    public void run() throws IOException {
        try (BufferedReader reader = new BufferedReader(new StringReader(text().get()))) {
            avg().set(reader.lines().collect(Collectors.averagingInt(String::length)));
        }
    }
}
