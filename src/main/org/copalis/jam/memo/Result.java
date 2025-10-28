package org.copalis.jam.memo;

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
    public boolean modified() {
        return signature.modified() || Mutable.hasChanged(value) || dependencies.stream().anyMatch(Mutable::hasChanged);
    }
}
