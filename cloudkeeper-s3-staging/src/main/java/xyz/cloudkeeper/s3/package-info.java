/**
 * Provides a serializing staging area backed by Amazon S3.
 *
 * <p>The main user-facing class of this package is
 * {@link xyz.cloudkeeper.s3.S3StagingArea.Builder}, which is a builder for
 * {@link xyz.cloudkeeper.simple.staging.ExternalStagingArea} instances backed by Amazon S3.
 *
 * <p>An S3 staging area needs to map execution traces to keys (or key prefixes) on S3: Specifically, each key generated
 * and accessed by this staging area consists of three parts:
 * <ul><li>
 *     The staging-area prefix, which is the same for every execution trace.
 * </li><li>
 *     The part dependent on the execution trace without the reference.
 * </li><li>
 *     The execution-trace reference and marshaling-dependent part.
 * </li></ul>
 */
@NonNullByDefault
package xyz.cloudkeeper.s3;

import xyz.cloudkeeper.model.util.NonNullByDefault;
