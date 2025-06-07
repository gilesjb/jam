package org.copalis.jam;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Instantiates an interface using a dynamic proxy and memoizes the method calls.
 * Memoized results can be saved to a cache file and restored later.
 *
 * @author gilesjb
 */
public class Memorizer {

    private final LinkedList<Set<Mutable>> dependencies = new LinkedList<>();
    private Map<Invocation, Result> cache = new LinkedHashMap<>();
    private final Observer observer;

    /**
     * Creates an instance
     * @param observer an invocation observer, must not be null
     */
    public Memorizer(Observer observer) {
        this.observer = Objects.requireNonNull(observer);
    }

    /**
     * Creates an instance with no related observer or cache file
     */
    public Memorizer() {
        this(new Observer() { });
    }

    enum Status {
        COMPUTE, REFRESH, CURRENT
    }

    interface Observer {
        default void startMethod(Status status, Method method, List<Object> params) { };
        default Object endMethod(Status status, Method method, List<Object> params, Object result) {
            return result;
        }
    }

    /**
     * The signature of a method call
     * @param name the name of an invoked method
     * @param params the parameters passed in the method call
     */
    public record Invocation(String name, List<Object> params) implements Mutable {
        Invocation(Method method, Object... params) {
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

    private static boolean objSerializable(Object obj) {
        return Objects.isNull(obj) || obj instanceof Serializable;
    }

    /**
     * The result of a method call
     * @param signature the method call signature
     * @param value the method call result
     * @param sources the dependencies of the method call
     */
    public record Result(Invocation signature, Object value, Set<Mutable> sources) implements Mutable {
        boolean serializable() {
            return objSerializable(value) && signature.serializable();
        }
        public boolean modified() {
            return signature.modified() || Mutable.hasChanged(value) || sources.stream().anyMatch(Mutable::hasChanged);
        }
    }

    void load(InputStream in) throws IOException, ClassNotFoundException {
        try (ObjectInputStream obj = new ObjectInputStream(in)) {
            @SuppressWarnings("unchecked")
            List<Result> results = (List<Result>) obj.readObject();
            results.forEach(result -> cache.put(result.signature(), result));
        }
    }

    void save(OutputStream out) throws IOException {
        try (ObjectOutputStream obj = new ObjectOutputStream(out)) {
            obj.writeObject(cache.values().stream().filter(Result::serializable)
                    .collect(Collectors.toList()));
        }
    }

    /**
     * Gets the cache contents
     * @return a stream of cached method results
     */
    public Stream<Result> entries() {
        return cache.values().stream();
    }

    /**
     * Looks up a cache entry
     * @param invocation the method call
     * @return a Result or null
     */
    public Result lookup(Invocation invocation) {
        return cache.get(invocation);
    }

    /**
     * Erases all method calls from the cache and deletes the cache file
     */
    public void forget() {
        cache = new LinkedHashMap<>();
    }

    /**
     * Records a resource as being a dependency of the current method that is executing
     * @param resource the resource
     */
    public void dependsOn(Mutable resource) {
        dependencies.peek().add(resource);
    }

    /**
     * Creates a memoized instance of an interface
     * @param <T> the build type
     * @param t the Java class of the interface
     * @return the created object
     */
    public <T> T instantiate(Class<T> t) {
        dependencies.push(new HashSet<>());
        return t.cast(Proxy.newProxyInstance(t.getClassLoader(), new Class[]{t}, this::invokeMethod));
    }

    private Object invokeMethod(Object proxy, Method method, Object[] args)
            throws Throwable {
        Invocation signature = new Invocation(method, args);

        Status status = Status.COMPUTE;
        if (cache.containsKey(signature)) {
            Result result = cache.get(signature);

            if (result.modified()) {
                status = Status.REFRESH;
                cache.remove(signature);
            } else {
                observer.startMethod(Status.CURRENT, method, signature.params());
                dependencies.peek().addAll(result.sources());
                observer.endMethod(Status.CURRENT, method, signature.params(), result.value());
                return result.value();
            }
        }
        dependencies.push(new HashSet<>());
        observer.startMethod(status, method, signature.params());

        Object value = null;
        try {
            value = observer.endMethod(status, method, signature.params(),
                    InvocationHandler.invokeDefault(proxy, method, args));
            if (method.getReturnType() != Void.TYPE) {
                cache.put(signature, new Result(signature, value, dependencies.peek()));
            }
            return value;
        } finally {
            Set<Mutable> used = dependencies.pop();
            dependencies.peek().addAll(used);
        }
    }
}
