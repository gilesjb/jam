package org.copalis.jam.memo;

import java.lang.reflect.Method;
import java.util.List;

/**
 * A method invocation observer that is allowed to modify return values
 */
public interface Observer {
    /**
     * Represents the cache status for a method invocation
     */
    enum Status {
        /**
         * The method must be executed as there is no cached result for the invocation
         */
        COMPUTE,
        /**
         * The method must be executed because the cached result is stale
         */
        REFRESH,
        /**
         * The cache contains a fresh result for the method invocation
         */
        CURRENT
    }

    /**
     * Notification that a method is about to be invoked
     * @param status the cache status
     * @param method the method
     * @param params the method parameter values
     */
    default void startMethod(Status status, Method method, List<Object> params) { };
    /**
     * Notification that a method has completed
     * @param status the cache status
     * @param method the method
     * @param params the method parameter values
     * @param result the value that was returned by the method or retrieved from cache
     * @return the value that should be returned
     */
    default Object endMethod(Status status, Method method, List<Object> params, Object result) {
        return result;
    }
}