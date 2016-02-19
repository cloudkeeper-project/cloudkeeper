package xyz.cloudkeeper.model.runtime.element.module;

import xyz.cloudkeeper.model.bare.element.module.BareInPort;
import xyz.cloudkeeper.model.util.ImmutableList;

import java.util.List;

public interface RuntimeInPort extends RuntimePort, BareInPort {
    /**
     * Returns the index of this in-port in the list of all its enclosing module's in-ports.
     *
     * <p>The result of this method is the index of this in-port in the {@link List} that is obtained by calling
     * {@link RuntimePortContainer#getInPorts()} on {@link #getEnclosingElement()}).
     *
     * @return index of this in-port in the list of all its enclosing module's in-ports
     */
    int getInIndex();

    /**
     * Returns a list of out-ports (from {@code getEnclosingElement().getOutPorts()}) that depend on this in-port.
     *
     * <p>An out-port in {@code getEnclosingElement().getOutPorts()} is guaranteed to be included in the returned list
     * if the value for this in-port is needed for computing the value for the out-port. Implementations are free to
     * include (or not to include) out-ports that are not influenced by the value of this in-port.
     *
     * @return list of out-ports that depend on this in-port
     */
    ImmutableList<? extends RuntimeOutPort> getDependentOutPorts();

    /**
     * Returns the equivalence class of this in-port, with respect to the equivalence relation induced by
     * {@link #getDependentOutPorts()}.
     *
     * Method {@link #getDependentOutPorts()} induces an equivalence relation on the set of in-ports as follows:
     * Let {@code a} be an in-port. Compute the sequence of sets {@code S[0], S[1], ...} of in-ports as follows:
     * <ol><li>
     *     {@code S[0]} contains only {@code a}.
     * </li><li>
     *     {@code S[i + 1]} is obtained by first computing the union {@code U} of all out-ports returned by
     *     {@link #getDependentOutPorts()}, over all in-ports in {@code S[i]}, and then "inverting" {@code U} with
     *     respect to {@link #getDependentOutPorts()}; that is, computing all in-ports for which
     *     {@link #getDependentOutPorts()} would include an out-port from {@code U}.
     * </li></ol>
     * Since the sets {@code S[i]} are growing, and there are only a finite number of in-ports, this sequence converges
     * to some limit set {@code S}.
     *
     * This method returns all ports contained in the limit set {@code S} when the initial in-port {@code a} is the
     * current in-port.
     *
     * Let {@code ~} be the binary relation defined by {@code a ~ b} iff {@code a} and {@code b} have the same limit
     * set. Then {@code ~} is an equivalence relation. That is, {@code ~} satisfies reflexivity, symmetry, and
     * transitivity.
     *
     * By definition of equivalence relations, each in-port belongs to exactly one equivalence class.
     *
     * @return All in-ports in the equivalence class of this in-port. This method returns an equal list when applied to
     *     any of the in-ports returned by this method. That is, {@link #equals(Object)} method would return
     *     {@code true}.
     */
    // TODO: Remove?
    // RuntimeList<? extends RuntimeInPort> getInPortEquivalenceClass();
}
