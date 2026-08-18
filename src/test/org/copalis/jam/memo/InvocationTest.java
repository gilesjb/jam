package org.copalis.jam.memo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

class InvocationTest {

    @Test
    void testVarArgs() throws NoSuchMethodException, SecurityException {
        Method printf = PrintStream.class.getDeclaredMethod("printf", String.class, Object[].class);
        Invocation invocation = new Invocation(printf, "%d,%s", new Object[] {5, 10});
        assertEquals(Arrays.asList("%d,%s", 5, 10), invocation.params());
    }
}
