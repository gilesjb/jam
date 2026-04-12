package org.copalis.jam.memo;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A reference to one or more resources that might have been modified since the reference was created
 *
 * @author gilesjb
 */
public interface Mutable extends Serializable {

    /**
     * A Mutable instance that always returns true from {@link #modifiedSince(Serializable)}
     */
    static final Mutable CHANGED = () -> new Serializable() {

        private static final long serialVersionUID = 1L;
        @Override public boolean equals(Object other) {
            return false;
        }
    };

    /**
     * Returns a serializable representation of the current state of the resources
     * @return an object
     */
    Serializable currentState();

    /**
     * Compares current and previous state
     * @param oldState a previous state returned by {@link #currentState()}
     * @return true if the current state is different to the old one
     */
    default boolean modifiedSince(Serializable oldState) {
        return !Objects.equals(oldState, currentState());
    }

    /**
     * Returns an object's current state
     * @param obj the object
     * @return the object's current state if it is mutable, otherwise null
     */
    static Serializable currently(Object obj) {
        return obj instanceof Mutable mut ? mut.currentState() : null;
    }

    /**
     * Returns a representation of the current state of a stream of resources
     * @param values the stream of objects
     * @return a map of objects to their current state
     */
    static Serializable snapshots(Stream<?> values) {
        return values.collect(Collectors.toMap(Function.identity(), Mutable::currently, (a, b) -> a, HashMap::new));
    }
}
