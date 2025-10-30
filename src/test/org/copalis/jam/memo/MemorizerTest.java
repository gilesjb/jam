package org.copalis.jam.memo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class MemorizerTest {

    interface Fibonacci {
        default long fib(long x) {
            return x <= 1 ? x : fib(x - 1) + fib(x - 2);
        }
    }

    @Test public void testMemoization() {
        List<Observer.Status> calls = new LinkedList<>();

        Memorizer memo = new Memorizer(new Observer() {
            public void startMethod(Observer.Status status, Method method, List<Object> params) {
                calls.add(status);
            }
        });

        Fibonacci fib = memo.instantiate(Fibonacci.class);

        assertEquals(3736710778780434371L, fib.fib(100));
        assertEquals(101, calls.stream().filter(Observer.Status.COMPUTE::equals).count());
        assertEquals(98, calls.stream().filter(Observer.Status.CURRENT::equals).count());

        calls.clear();
        assertEquals(3736710778780434371L, fib.fib(100));
        assertEquals(1, calls.size());
    }

    interface Foo {
        void foo();
    }

    @Test public void testAbstractMethod() {
        Memorizer memo = new Memorizer();
        Foo foo = memo.instantiate(Foo.class);

        assertThrowsExactly(IllegalArgumentException.class, foo::foo);
    }

    static boolean changed = false;

    static final Mutable mutable = () -> changed;

    interface Mutables {
        default String a() {
            aa();       // no prior dependencies or parameters
            ab("foo");  // records a dependency
            ac();       // no parameters, so can't be affected by ab's dependency
            ad("foo");  // one parameter, so could be affected by ab's dependency
            ae(() -> false);
            return "Done";
        }

        default String aa() {
            return "";
        }

        default Mutable ab(String foo) {
            return mutable;
        }

        default String ac() {
            return "";
        }

        default String ad(String foo) {
            return "";
        }

        default String ae(Mutable m) {
            return "";
        }
    }

    @Test synchronized public void testMutation() {
        List<String> called = new LinkedList<>();

        Memorizer memo = new Memorizer(new Observer() {
            public void startMethod(Observer.Status status, Method method, List<Object> params) {
                if (status == Observer.Status.COMPUTE || status == Observer.Status.REFRESH) {
                    called.add(method.getName());
                }
            }
        });

        changed = false;

        Mutables t = memo.instantiate(Mutables.class);
        t.a();
        assertEquals(List.of("a", "aa", "ab", "ac", "ad", "ae"), called);
        assertTrue(memo.entries().noneMatch(Result::modified));

        called.clear();
        t.a();
        assertEquals(List.of(), called);
        assertTrue(memo.entries().noneMatch(Result::modified));

        changed = true;
        assertTrue(memo.entries().anyMatch(Result::modified));

        called.clear();
        t.a();
        assertEquals(List.of("a", "ab", "ad"), called);
    }
}
