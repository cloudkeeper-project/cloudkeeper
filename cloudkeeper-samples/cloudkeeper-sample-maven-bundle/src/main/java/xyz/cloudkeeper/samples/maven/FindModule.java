package xyz.cloudkeeper.samples.maven;

import xyz.cloudkeeper.dsl.SimpleModule;
import xyz.cloudkeeper.dsl.SimpleModulePlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

@SimpleModulePlugin("Determines whether a text contains a substring")
public abstract class FindModule extends SimpleModule<FindModule> {
    public abstract InPort<String> text();
    public abstract InPort<String> substring();
    public abstract OutPort<Boolean> wasFound();

    @Override
    public void run() throws IOException {
        String substring = substring().get();
        try (BufferedReader reader = new BufferedReader(new StringReader(text().get()))) {
            wasFound().set(reader.lines().anyMatch(line -> line.contains(substring)));
        }
    }
}
