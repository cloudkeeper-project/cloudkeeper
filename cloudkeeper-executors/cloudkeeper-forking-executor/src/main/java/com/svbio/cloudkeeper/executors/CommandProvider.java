package com.svbio.cloudkeeper.executors;

import com.svbio.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;

import java.util.List;

/**
 * Provider of command lines in order to execute a simple-module executor process.
 */
public interface CommandProvider {
    /**
     * Returns the command that will be executed for the given execution trace.
     *
     * <p>The returned command is suitable to be passed to {@link ProcessBuilder#ProcessBuilder(List)}. Implementations
     * of this interface <em>must</em> be thread-safe. They <em>should</em> be purely functional, without side-effects
     * and without mutable state. Typically, the result returned by this method only depends on the annotations
     * available through {@code trace}.
     *
     * @param executionTrace absolute execution trace, representing the simple module
     * @return the command that will be executed for the given execution trace; must not be null
     * @throws NullPointerException if the argument is null
     */
    List<String> getCommand(RuntimeAnnotatedExecutionTrace executionTrace);
}
