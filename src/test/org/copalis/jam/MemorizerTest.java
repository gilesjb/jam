package org.copalis.jam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import org.copalis.jam.Memorizer.Result;
import org.copalis.jam.Memorizer.Status;
import org.junit.jupiter.api.Test;

public class MemorizerTest {

    interface Fibonacci {
        default long fib(long x) {
            return x <= 1 ? x : fib(x - 1) + fib(x - 2);
        }

        void foo();
    }

    @Test public void testMemoization() {
        List<Status> calls = new LinkedList<>();

        Memorizer memo = new Memorizer(new Memorizer.Observer() {
            public void startMethod(Status status, Method method, List<Object> params) {
                calls.add(status);
            }
        });

        Fibonacci fib = memo.instantiate(Fibonacci.class);


        assertEquals(3736710778780434371L, fib.fib(100));
        assertEquals(101, calls.stream().filter(Status.COMPUTE::equals).count());
        assertEquals(98, calls.stream().filter(Status.CURRENT::equals).count());

        calls.clear();
        assertEquals(3736710778780434371L, fib.fib(100));
        assertEquals(1, calls.size());
    }

    @Test public void testAbstractMethod() {
        Memorizer memo = new Memorizer();
        Fibonacci fib = memo.instantiate(Fibonacci.class);

        assertThrowsExactly(IllegalArgumentException.class, fib::foo);
    }

    static boolean changed = false;

    static Mutable mutable = () -> changed;

    interface Mutables {
        default String a(Memorizer m) {
            String aa = aa();  // no prior dependencies or parameters
            String ab = ab(m); // records a dependency
            String ac = ac();  // no parameters, so can't be affected by ab's dependency
            String ad = ad(m); // one parameter, so could be affected by ab's dependency
            return aa + ab + ac + ad;
        }

        default String aa() {
            return "aa";
        }

        default String ab(Memorizer m) {
            m.dependsOn(mutable);
            return "ab";
        }

        default String ac() {
            return "ac";
        }

        default String ad(Memorizer m) {
            return "ad";
        }
    }

    @Test synchronized public void testMutation() {
        List<String> called = new LinkedList<>();

        Memorizer memo = new Memorizer(new Memorizer.Observer() {
            public void startMethod(Status status, Method method, List<Object> params) {
                if (status == Status.COMPUTE || status == Status.REFRESH) {
                    called.add(method.getName());
                }
            }
        });

        changed = false;

        Mutables t = memo.instantiate(Mutables.class);
        assertEquals("aaabacad", t.a(memo));
        assertEquals(List.of("a", "aa", "ab", "ac", "ad"), called);
        assertTrue(memo.entries().noneMatch(Result::modified));

        called.clear();
        assertEquals("aaabacad", t.a(memo));
        assertEquals(List.of(), called);
        assertTrue(memo.entries().noneMatch(Result::modified));

        changed = true;
        assertTrue(memo.entries().anyMatch(Result::modified));

        called.clear();
        assertEquals("aaabacad", t.a(memo));
        assertEquals(List.of("a", "ab", "ad"), called);
    }
}
