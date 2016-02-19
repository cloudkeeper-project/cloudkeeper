package xyz.cloudkeeper.drm;

import xyz.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;

/**
 * Provider of DRMAA native specifications; that is, "an opaque string that is passed [...] to DRMAA to specify
 * site-specific resources and/or policies".
 *
 * @see org.ggf.drmaa.JobTemplate#setNativeSpecification(String)
 */
public interface NativeSpecificationProvider {
    /**
     * Returns the DRMAA native specifications that should be passed to
     * {@link org.ggf.drmaa.JobTemplate#setNativeSpecification(String)}.
     *
     * <p>Implementations of this interface should be purely functional, without side-effects and without mutable state.
     * Typically, the result returned by this method only depends on the annotations available through {@code trace}.
     *
     * @param trace absolute execution trace corresponding to the simple module that will be executed
     * @return the native specification, must not be null
     * @throws NullPointerException if the argument is null
     */
    String getNativeSpecification(RuntimeAnnotatedExecutionTrace trace);
}
