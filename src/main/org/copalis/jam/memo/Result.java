package org.copalis.jam.memo;

import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

/**
 * The result of a method call
 * @param signature the method call signature
 * @param value the method call result
 * @param dependencies the dependencies of the method call
 * @param state if the value is mutable, its current state
 */
public record Result(Invocation signature, Object value, Set<Result> dependencies, Serializable state) implements Serializable {

    public Result(Invocation signature, Object value, Set<Result> dependencies) {
        this(signature, value, dependencies, value instanceof Mutable m ? m.currentState() : null);
    }

    boolean serializable() {
        return Memorizer.objSerializable(value) && signature.serializable();
    }

    /**
     * Checks if this result is stale and needs to be recomputed
     * @return true if it is stale
     */
    public boolean isStale() {
        return value instanceof Mutable && Objects.isNull(state); // || dependencies.stream().anyMatch(Result::isStale);
    }

    /**
     * Checks if this result is still valid
     * @return an updated version of this result indicating if it needs to be recomputed
     */
    Result currently() {
        if (value instanceof Mutable mut) {
            return mut.modifiedSince(state) ? new Result(signature, value, dependencies, null) : this;
        } else {
            return this;
        }
    }

    @Override public int hashCode() {
        return signature.hashCode();
    }

    @Override public boolean equals(Object other) {
        return other instanceof Result res && Objects.equals(signature, res.signature);
    }
}
