package com.svbio.cloudkeeper.interpreter;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import com.svbio.cloudkeeper.interpreter.DependencyGraph.HasValue;
import com.svbio.cloudkeeper.model.api.staging.StagingArea;

import java.io.Serializable;
import java.util.BitSet;
import java.util.List;

@FunctionalInterface
interface InterpreterPropsProvider extends Serializable {
    /**
     * Returns an Akka actor {@link Creator} (factory) for producing {@link UntypedActor} instances that implement the
     * {@link InterpreterInterface} message interface.
     *
     * <p>The given {@link BitSet} objects are guaranteed to remain unmodified; hence, it is not necessary for the
     * caller to create defensive copies.
     *
     * @param interpreterProperties interpreter properties that apply to all module interpreter actors (in the current
     *     JVM)
     * @param stagingArea staging area (which also represents the current module and the current execution trace)
     * @param moduleId id of the module that will be used by the actor when sending
     *     {@link InterpreterInterface.SubmoduleOutPortHasSignal} messages
     * @param inPortsHasValueList List of {@link HasValue} instances, for each in-port, that specify if the state of
     *     each in-port is unknown, whether it has a value, or if it has no value. The list must not contain
     *     {@link HasValue#PENDING_VALUE_CHECK}.
     * @param recomputedInPorts set of in-ports for which explicit {@link InterpreterInterface.InPortHasSignal} messages
     *     will be sent to the new actor
     * @param requestedOutPorts set of out-port for which the parent actor of the newly created actor expects
     *     {@link InterpreterInterface.SubmoduleOutPortHasSignal} messages
     * @return the Akka actor {@link Creator}
     * @throws NullPointerException if any of the arguments is null
     * @throws IllegalArgumentException if any of the arguments are invalid; for instance, if {@code recomputedInPorts}
     *     or {@code requestedOutPorts} are invalid sets because contains indices that do not correspond to ports, or
     *     because {@code requestedOutPorts} is empty
     */
    Props provideInterpreterProps(
        LocalInterpreterProperties interpreterProperties,
        StagingArea stagingArea,
        int moduleId,
        List<HasValue> inPortsHasValueList,
        BitSet recomputedInPorts,
        BitSet requestedOutPorts
    );
}
