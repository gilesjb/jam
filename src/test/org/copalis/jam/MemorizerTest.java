package org.copalis.jam;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import org.copalis.jam.Memorizer.Status;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MemorizerTest {

    public interface Fibonacci {
        default long fib(long x) {
            return x <= 1 ? x : fib(x - 1) + fib(x - 2);
        }

        void foo();
    }

    @Test public void testMemoization() {
        Memorizer memo = new Memorizer();
        Fibonacci fib = memo.instantiate(Fibonacci.class);

        List<Status> calls = new LinkedList<>();

        memo.setListener(new Memorizer.Listener() {
            public void startMethod(Status status, Method method, List<Object> params) {
                calls.add(status);
            }
        });
        assertEquals(3736710778780434371L, fib.fib(100));
        assertEquals(101, calls.stream().filter(Status.EXECUTE::equals).count());
        assertEquals(98, calls.stream().filter(Status.CURRENT::equals).count());

        calls.clear();
        assertEquals(3736710778780434371L, fib.fib(100));
        assertEquals(1, calls.size());
    }

    @Test public void testAbstractMethod() {
        Memorizer memo = new Memorizer();
        Fibonacci fib = memo.instantiate(Fibonacci.class);

        Assertions.assertThrowsExactly(IllegalArgumentException.class, () -> {
            fib.foo();
        });
    }
}
