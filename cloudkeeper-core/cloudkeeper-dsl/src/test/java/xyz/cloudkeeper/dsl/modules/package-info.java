/**
 * This package exists because proper error reporting relies on that module classes are defined outside of
 * package {@link xyz.cloudkeeper.plugins.dsl}. More precisely, the result returned by
 * {@link xyz.cloudkeeper.dsl.Locatables#getCallingStackTraceElement()} depends on the packages of the
 * classes in the stack trace.
 */
package xyz.cloudkeeper.dsl.modules;
