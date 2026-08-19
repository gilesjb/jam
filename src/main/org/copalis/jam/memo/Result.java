package org.copalis.jam.memo;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The result of a method call
 * @param readFileCall the method call signature
 * @param value the method call result
 * @param dependencies the dependencies of the method call
 */
public record Result(Invocation signature, Object value, Set<Mutable> dependencies) implements Mutable {
    boolean serializable() {
        return Memorizer.objSerializable(value) && signature.serializable();
    }

    /**
     * Indicates whether this result is up to date with mutable state
     * @param states a map of mutable states
     * @return true if this result is up to date
     */
    public boolean isCurrent(Map<Mutable, Serializable> states) {
        // have any parameter states changed?
        if (!signature.isCurrent(states)) return false;
        
        // have any transitive dependencies changed?
        for (Mutable dependency : dependencies) {
            Serializable prevState = states.get(dependency);
            Serializable currentState = dependency.currentState();
            if (!Objects.equals(currentState, prevState)) return false;
        }
        
        // has the return value changed?
        if (!(value instanceof Mutable m)) return true;
        if (!Objects.equals(m.currentState(), states.get(value))) return false;
        return true;
    }

    public Serializable currentState() {
        return new LinkedList<>(Arrays.asList(signature.currentState(), Mutable.currently(value), Mutable.snapshots(dependencies.stream())));
    }
}
