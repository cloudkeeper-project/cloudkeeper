/**
 * Defines interfaces and exceptions for simple-module executors.
 *
 * <p>The CloudKeeper interpreter component is responsible for traversing the runtime representation of a workflow (that
 * is, the optimized abstract-syntax tree) and recursively starting interpreters for every module as soon as all inputs
 * become available. Simple modules are atomic entities for the interpreter, as they consist of user-defined code
 * written in Java or some other programming language. Therefore, whenever a simple module is encountered during the
 * workflow interpretation, it is passed to the <em>simple-module executor</em>, which is responsible for calling the
 * user-defined code. A simple-module executor has a great degree of freedom in doing so: For instance, it may call
 * the user-defined code on a remote machine, make use of external schedulers, etc.
 *
 * <p>This package defines the CloudKeeper simple-module executor interface, along with other interfaces that support
 * implementing simple-module executors using standardized components. The suggested sequence of steps for a
 * simple-module executor built using standard components is:
 *
 * <ul><li>
 *     Use the {@link xyz.cloudkeeper.model.api.staging.StagingAreaProvider} instance passed to
 *     {@link xyz.cloudkeeper.model.api.executor.SimpleModuleExecutor#submit(xyz.cloudkeeper.model.api.RuntimeStateProvider, scala.concurrent.Future)}
 *     for reconstructing the runtime state.
 * </li><li>
 *     Use a {@link xyz.cloudkeeper.model.api.executor.ModuleConnectorProvider} instance for creating the
 *     {@link xyz.cloudkeeper.model.api.executor.ExtendedModuleConnector} instance and pass it to
 *     {@link xyz.cloudkeeper.model.api.Executable#run(xyz.cloudkeeper.model.api.ModuleConnector)}.
 *     Afterward, use {@link xyz.cloudkeeper.model.api.executor.ExtendedModuleConnector#commit()} to write the
 *     outputs to the staging area.
 * </li></ul>
 */
@NonNullByDefault
package xyz.cloudkeeper.model.api.executor;

import xyz.cloudkeeper.model.util.NonNullByDefault;
