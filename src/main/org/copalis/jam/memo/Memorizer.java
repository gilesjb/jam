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
import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A method memoizer that can also determine when methods need to be re-executed as a result of
 * changes to external resources.
 * <p>
 * The Memorizer creates instances of interfaces and instruments them with a method handler which
 * intercepts calls to {@code default} methods.
 * <ul>
 * <li>If a cache entry exists for a method call, the result of that earlier call is returned.
 * <li>If there is no cache entry for a method call the method is executed,
 * and its result is stored in the memoizer's cache if it is eligible to be cached
 * </ul>
 * <p>
 * The state of the cache can also be saved and loaded,
 * excluding method calls non-{@link java.io.Serializable} parameters or return value.
 *
 * <h2>Cache eligibility</h2>
 * <ul>
 * <li>Methods that do not have return type {@code void} are cacheable, and should only perform idempotent actions
 * <li>Methods with {@code void} return type are not cacheable, and may be used for logic with side-effects
 * </ul>
 *
 * <h2>Staleness checking</h2>
 * The method handler performs <i>staleness checking</i> on cached method calls which return or depend on
 * references to mutable resources such as files.
 * To enable staleness checking, objects which reference resources must implement {@link Mutable}.
 * A resource reference object is <i>modified</i> if its {@link Mutable#currentState} value changes.
 *<p>
 * A cached method call is <i>stale</i> if any of these conditions are met:
 * <ul>
 * <li>Its returned value is modified
 * <li>It made a method call which is stale
 * <li>One or more of its parameters are modified
 * <li>It has one or more non-{@link Mutable} parameters
 * AND a previous call within the scope of the method calling it returned a modified value
 * </ul>
 *
 * If a cache entry is stale, the method invocation will be executed as though it was not cached.
 *
 * @author gilesjb
 */
public class Memorizer {

    // persisted state
    private final Map<Mutable, Serializable> states = new IdentityHashMap<>();
    private final Map<Invocation, Result> results = new LinkedHashMap<>();
    private final Map<Object, Result> sources = new IdentityHashMap<>();

    // local state
    private final Observer observer;
    private final LinkedList<Set<Mutable>> dependencies = new LinkedList<>();
    private long resultId = 1L;

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
     * Loads the cache
     * @param in an input stream the serialized cache contents will be read from
     * @throws IOException if an IO exception occurs
     * @throws ClassNotFoundException if a serialized class cannot be found
     */
    @SuppressWarnings("unchecked")
    public void load(InputStream in) throws IOException, ClassNotFoundException {
        try (ObjectInputStream obj = new ObjectInputStream(in)) {
            Map<Mutable, Serializable> loadedStates = (Map<Mutable, Serializable>) obj.readObject();
            states.clear();
            loadedStates.forEach((mutable, prevState) -> {
                if (!mutable.modifiedSince(prevState)) {
                    states.put(mutable, prevState);
                }
            });
            results.clear();
            ((List<Result>) obj.readObject()).forEach(result -> results.put(result.signature(), result));
            sources.clear();
            ((List<Result>) obj.readObject()).forEach(result -> sources.put(result.value(), result));
        }
    }

    /**
     * Writes the contents of the method call cache to an output stream,
     * excluding method calls which are not serializable.
     *
     * @param out an output stream the serialized cache contents will be written to
     * @throws IOException if an IO exception occurs
     */
    public void save(OutputStream out) throws IOException {
        try (ObjectOutputStream obj = new ObjectOutputStream(out)) {
            obj.writeObject(states);
            obj.writeObject(results.values().stream().filter(Result::serializable)
                    .collect(Collectors.toList()));
            obj.writeObject(sources.values().stream().filter(Result::serializable)
                    .collect(Collectors.toList()));
        }
    }

    /**
     * Gets the method call results currently in the cache
     * @return a stream of result objects
     */
    public Stream<Result> cacheEntries() {
        return results.values().stream();
    }

    /**
     * Finds a cached invocation result
     * @param invocation the method invocation to find
     * @return the cached invocation result, if it exists
     */
    public Result findResult(Invocation invocation) {
        return results.get(invocation);
    }

    /**
     * Checks is a cached method result value is current
     * @param result the result to check
     * @return true if the result is non-null and fresh
     */
    public boolean isResultCurrent(Result result) {
        return Objects.nonNull(result) && result.isCurrent(states);
    }

    /**
     * Gets the method Result that a value originated from
     * @param value the value to look up
     * @return the method Result it originated from, if it exists
     */
    public Result sourceResult(Object value) {
        return sources.get(value);
    }

    /**
     * Erases all method call results from the cache
     */
    public void forget() {
        results.clear();
        states.clear();
        resultId = 1L;
    }

    /**
     * Creates a memoized instance of an interface.
     * The methods declared by the interface must all have a <code>default</code> implementation.
     * @param <T> the interface type to instantiate
     * @param t the Java class of the interface
     * @return an instance of the interface
     */
    public <T> T instantiate(Class<T> t) {
        dependencies.push(new HashSet<>());
        return t.cast(Proxy.newProxyInstance(t.getClassLoader(), new Class[]{t}, this::invokeMethod));
    }

    private Object invokeMethod(Object proxy, Method method, Object[] args)
            throws Throwable {
        final Invocation signature = new Invocation(method, args);
        final Result cached = results.get(signature);

        if (Objects.nonNull(cached) && cached.isCurrent(states)) {
            observer.startMethod(Observer.Status.CURRENT, method, signature.params(), cached);
            observer.endMethod(Observer.Status.CURRENT, method, signature.params(), cached.value());
            dependencies.peek().addAll(cached.dependencies());
            return cached.value();
        }

        final Class<?> returnType = method.getReturnType();
        final Observer.Status status;

        if (Objects.nonNull(cached)) {
            results.remove(signature);
            status = Observer.Status.REFRESH;
        } else if (returnType == Void.TYPE) {
            status = Observer.Status.EXECUTE;
        } else {
            status = Observer.Status.COMPUTE;
        }

        if (Arrays.stream(method.getParameterTypes()).allMatch(Mutable.class::isAssignableFrom)) {
            dependencies.push(new LinkedHashSet<>());
        } else { // propagate dependencies to invoked method if it has params without version info
            dependencies.push(new LinkedHashSet<>(dependencies.peek()));
        }

        try {
            observer.startMethod(status, method, signature.params(), cached);
            final Object returnValue = observer.endMethod(status, method, signature.params(),
                    InvocationHandler.invokeDefault(proxy, method, args));
            if (returnType != Void.TYPE) {
                final Result result = new Result(resultId++, signature, returnValue, new LinkedHashSet<>(dependencies.peek()));
                results.put(signature, result);
                if (Objects.nonNull(returnValue) && !sources.containsKey(returnValue)) {
                    sources.putIfAbsent(returnValue, result);
                }
            }
            if (Mutable.class.isAssignableFrom(returnType)) {
                if (Objects.nonNull(returnValue)) {
                    final Mutable m = (Mutable) returnValue;
                    dependencies.peek().add(m);
                    states.computeIfAbsent(m, Mutable::currentState);
                } else {
                    dependencies.peek().add(Mutable.CHANGED);
                }
            }
            return returnValue;
        } finally {
            Set<Mutable> used = dependencies.pop();
            dependencies.peek().addAll(used);
        }
    }
}
