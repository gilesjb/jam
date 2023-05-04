package org.copalis.jam;

import java.io.Serializable;

/**
 * A reference to a resource that might have been modified
 *
 * @author gilesjb
 */
public interface Memorizable extends Serializable {

    /**
     * Checks if the resource has been modified since this reference was created
     * @return true if the resource has not been modified
     */
    boolean current();

    /**
     * Checks if an object is a reference to a resource that has been modified
     * @param obj an object that might be modifiable
     * @return true if the resource has not been modified
     */
    static boolean isCurrent(Object obj) {
        return !(obj instanceof Memorizable mem) || mem.current();
    }
}
