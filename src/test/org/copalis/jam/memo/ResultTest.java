package org.copalis.jam.memo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collections;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ResultTest {

    public Mutable readFile(String name) {
        fail("This is just a stub");
        return null;
    }

    static Invocation readFileCall;
    static Mutable foo = () -> "foo";

    @BeforeAll static public void setup() throws NoSuchMethodException, SecurityException {
        readFileCall = new Invocation(ResultTest.class.getDeclaredMethod("readFile", String.class), "foo.txt");
    }

    @Test
    void testParametersChanged() {
        Result readFileResult = new Result(readFileCall, foo, Collections.emptySet());

        assertFalse(readFileResult.isCurrent(Collections.emptyMap()));
        assertTrue(readFileResult.isCurrent(Collections.singletonMap(foo, "foo")));
        assertFalse(readFileResult.isCurrent(Collections.singletonMap(foo, "bar")));
    }
}
