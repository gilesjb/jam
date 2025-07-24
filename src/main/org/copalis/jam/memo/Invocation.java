package org.copalis.jam.memo;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * The signature of a method call
 * @param name the name of an invoked method
 * @param params the parameters passed in the method call
 */
public record Invocation(String name, List<Object> params) implements Mutable {
    /**
     * Creates an instance
     * @param method the invoked method
     * @param params the parameters passed in the method call
     */
    public Invocation(Method method, Object... params) {
        this(method.getName(), Objects.isNull(params) ? Collections.emptyList()
                : method.isVarArgs() ? expandVarArgs(params) : Arrays.asList(params));
    }

    private static List<Object> expandVarArgs(Object[] params) {
        List<Object> result = new LinkedList<>();
        for (int i = 0; i < params.length - 1; i++) {
            result.add(params[i]);
        }
        Object var = params[params.length - 1];
        for (int i = 0; i < Array.getLength(var); i++) {
            result.add(Array.get(var, i));
        }
        return result;
    }

    boolean serializable() {
        return params.stream().allMatch(Memorizer::objSerializable);
    }

    public boolean modified() {
        return params.stream().anyMatch(Mutable::hasChanged);
    }
}
