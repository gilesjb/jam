package org.copalis.jam.memo;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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
 * A memoizer that can create instances of interfaces.
 * It stores the results of calls to {@code default} methods on the instantiated interface and returns the cached
 * results when later calls to the methods are made with the same parameter values.
 * <p>
 * Cache entries that implement {@link java.io.Serializable} may be saved and loaded.
 * <p>
 * The memoizer can also track dependencies on external mutable state such as files,
 * and invalidate cache entries when that state changes.
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

    static boolean objSerializable(Object obj) {
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

    /**
     * Loads the cache
     * @param in an input stream the serialized cache contents will be read from
     * @throws IOException if an IO exception occurs
     * @throws ClassNotFoundException if a serialized class cannot be found
     */
    public void load(InputStream in) throws IOException, ClassNotFoundException {
        try (ObjectInputStream obj = new ObjectInputStream(in)) {
            @SuppressWarnings("unchecked")
            List<Result> results = (List<Result>) obj.readObject();
            results.forEach(result -> cache.put(result.signature(), result));
        }
    }

    /**
     * Saves the cache
     * @param out an output stream the serialized cache contents will be written to
     * @throws IOException if an IO exception occurs
     */
    public void save(OutputStream out) throws IOException {
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
     * Erases all method call results from the cache
     */
    public void forget() {
        cache = new LinkedHashMap<>();
    }

    /**
     * Records a mutable resource as a data dependency.
     * When a mutable resources's {@link Mutable#modified()} value is {@code true},
     * the return values of all dependent methods are invalidated.
     * <p>
     * When this method is called,
     * methods that may have a data dependency on the resource are recorded as dependents.
     * The exact rule for determining dependents is:
     * <ul>
     * <li>The method that called {@link #dependsOn(Mutable)}
     * <li>Any method that calls a dependent method
     * <li>Any method that has 1 or more parameters and is called from a method
     * that has previously called a dependent method
     * </ul>
     *
     * @param resource the mutable resource
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

        Observer.Status status = Observer.Status.COMPUTE;
        if (cache.containsKey(signature)) {
            Result result = cache.get(signature);

            if (result.modified()) {
                status = Observer.Status.REFRESH;
                cache.remove(signature);
            } else {
                observer.startMethod(Observer.Status.CURRENT, method, signature.params());
                dependencies.peek().addAll(result.sources());
                observer.endMethod(Observer.Status.CURRENT, method, signature.params(), result.value());
                return result.value();
            }
        }

        if (method.getParameterCount() > 0) {
            dependencies.push(new HashSet<>(dependencies.peek()));
        } else {
            dependencies.push(new HashSet<>());
        }
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
