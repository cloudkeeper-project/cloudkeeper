/**
 * Classes and interfaces providing high-level abstractions for common I/O operations on Amazon S3.
 *
 * <p>This package provides asynchronous wrappers around Amazon S3 primitives (basically wrappers around
 * {@link com.amazonaws.services.s3.AmazonS3} methods) and scalable high-performance input and output streams that are
 * suitable for use with streams of arbitrary size.
 *
 * <h2>Memory Consistency Properties</h2>
 *
 * The interfaces and classes provided by this package make use of execution services and futures. In many
 * circumstances, this frees callers from having to perform manual synchronization in order to achieve memory
 * consistency. Specifically, package {@link java.util.concurrent} details the circumstances under which a
 * <i>happens-before</i> relationship holds between operations in different threads.
 */
@NonNullByDefault
package com.svbio.cloudkeeper.s3.io;

import com.svbio.cloudkeeper.model.util.NonNullByDefault;
