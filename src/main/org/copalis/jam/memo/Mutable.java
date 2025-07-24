package org.copalis.jam.memo;

import java.io.Serializable;

/**
 * A reference to one or more resources that might have been modified since the reference was created
 *
 * @author gilesjb
 */
public interface Mutable extends Serializable {

    /**
     * Checks if the resource has been modified since this reference was created
     * @return true if the resource has been modified
     */
    boolean modified();

    /**
     * Checks if an object is a reference to a resource that has been modified
     * @param obj an object that might be modifiable
     * @return true if the resource has been modified
     */
    static boolean hasChanged(Object obj) {
        return obj instanceof Mutable mut && mut.modified();
    }
}
