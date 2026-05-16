package org.copalis.jam.memo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    static final Map<String, String> states = new HashMap<String, String>();

    record State(String key) implements Mutable {
        public String currentState() {
            return states.get(key);
        }
    }

    interface Project {
        default State get(String key) {
            return new State(key);
        }

        default State put(String key, String value) {
            states.put(key, value);
            return get(key);
        }

        default String name() {
            return "jam";
        }

        default State version() {
            return get("project-version");
        }

        default String projectName(State version) {
            return name() + '-' + version.currentState();
        }

        default State build() {
            return put("project-name", projectName(version()));
        }
    }

    Observer methodObserver(List<String> called) {
        return new Observer() {
            public void startMethod(Observer.Status status, Method method, List<Object> params) {
                if (status == Observer.Status.COMPUTE || status == Observer.Status.REFRESH) {
                    called.add(method.getName());
                }
            }
        };
    }

    @Test synchronized public void testDependencies() throws IOException, ClassNotFoundException {
        states.put("project-version", "1.0");

        List<String> called = new LinkedList<>();
        Memorizer memo;

        memo = new Memorizer(methodObserver(called));

        called.clear();
        assertEquals("jam-1.0", memo.instantiate(Project.class).build().currentState());
        assertEquals(List.of("build", "version", "get", "projectName", "name", "put", "get"), called);

        called.clear();
        assertEquals("jam-1.0", memo.instantiate(Project.class).build().currentState());
        assertEquals(List.of(), called);

        ByteArrayOutputStream saved = new ByteArrayOutputStream();
        memo.save(saved);

        // load cache into a new memorizer
        memo = new Memorizer(methodObserver(called));
        memo.load(new ByteArrayInputStream(saved.toByteArray()));

        called.clear();
        assertEquals("jam-1.0", memo.instantiate(Project.class).build().currentState());
        assertEquals(List.of(), called);

        // modify state and load a new memorizer
        states.put("project-version", "2.0");
        memo = new Memorizer(methodObserver(called));
        memo.load(new ByteArrayInputStream(saved.toByteArray()));

        called.clear();
        assertEquals("jam-2.0", memo.instantiate(Project.class).build().currentState());
        assertEquals(List.of("build", "version", "get", "projectName", "put", "get"), called);
    }
}
