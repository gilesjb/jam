package org.copalis.jam.memo;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The result of a method call
 * @param signature the method call signature
 * @param value the method call result
 * @param dependencies the dependencies of the method call
 */
public record Result(Invocation signature, Object value, Set<Mutable> dependencies) implements Mutable {
    boolean serializable() {
        return Memorizer.objSerializable(value) && signature.serializable();
    }
//    public boolean modified() {
//        return signature.modified() || Mutable.hasChanged(value) || dependencies.stream().anyMatch(Mutable::hasChanged);
//    }

    public boolean current(Map<Mutable, Serializable> states) {
        if (!(value instanceof Mutable)) return true;
        if (!Objects.equals(value, states.get(value))) return false;
        if (!signature.current(states)) return false;
        for (Mutable dependency : dependencies) {
            if (!Objects.equals(dependency.currentState(), states.get(dependency))) return false;
        }
        return true;
    }

    public Serializable currentState() {
        return new LinkedList<>(Arrays.asList(signature.currentState(), Mutable.currently(value), Mutable.snapshots(dependencies.stream())));
    }
}
