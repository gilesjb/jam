package org.copalis.builder;

import java.io.Serializable;

public interface Memorizable extends Serializable {

    boolean current();

    static boolean isCurrent(Object obj) {
        return !(obj instanceof Memorizable mem) || mem.current();
    }
}
