/**
 * This package exists because proper error reporting relies on that module classes are defined outside of
 * package {@link com.svbio.cloudkeeper.plugins.dsl}. More precisely, the result returned by
 * {@link com.svbio.cloudkeeper.dsl.Locatables#getCallingStackTraceElement()} depends on the packages of the
 * classes in the stack trace.
 */
package com.svbio.cloudkeeper.dsl.modules;
