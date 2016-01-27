package com.svbio.cloudkeeper.samples.maven;

import com.svbio.cloudkeeper.dsl.SimpleModule;
import com.svbio.cloudkeeper.dsl.SimpleModulePlugin;

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
