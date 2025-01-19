package org.copalis.jam;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Instantiates an interface using a dynamic proxy and memoizes the method calls.
 * Memoized results can be saved to a cache file and restored later.
 *
 * @author gilesjb
 */
public class Memorizer {
    /**
     * Creates an instance
     */
    public Memorizer() { }

    enum Status {
        COMPUTE, UPDATE, CURRENT
    }

    interface Listener {
        default void startMethod(Status status, Method method, List<Object> params) { }
        default void endMethod(Status status, Method method, List<Object> params, Object result) { }
    }

    /**
     * The signature of a method call
     * @param name the name of an invoked method
     * @param params the parameters passed in the method call
     */
    public record Invocation(String name, List<Object> params) implements Serializable {
        Invocation(Method method, Object[] params) {
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

        boolean isCurrent() {
            return params.stream().allMatch(Memorizable::isCurrent);
        }
    }

    /**
     * The result of a method call
     * @param signature the method call signature
     * @param value the method call result
     * @param sources the dependencies of the method call
     */
    public record Result(Invocation signature, Object value, Set<Memorizable> sources) implements Serializable {
        boolean isCurrent() {
            return signature.isCurrent() && Memorizable.isCurrent(value)
                    && sources.stream().allMatch(Memorizable::current);
        }
    }

    private final LinkedList<Set<Memorizable>> dependencies = new LinkedList<>();
    private Map<Invocation, Result> cache = new LinkedHashMap<>();
    private File cacheFile = null;
    private Listener listener = new Listener() { };

    void setListener(Listener listener) {
        this.listener = listener;
    }

    void setCacheFile(File file) {
        this.cacheFile = file;
    }

    void loadCache() throws IOException, ClassNotFoundException {
        try (InputStream in = new FileInputStream(cacheFile)) {
            try (ObjectInputStream obj = new ObjectInputStream(in)) {
                @SuppressWarnings("unchecked")
                HashMap<Invocation, Result> map = (HashMap<Invocation, Result>) obj.readObject();
                cache = map;
            }
        }
    }

    void save() throws FileNotFoundException, IOException {
        try (OutputStream out = new FileOutputStream(cacheFile)) {
            try (ObjectOutputStream obj = new ObjectOutputStream(out)) {
                obj.writeObject(cache);
            }
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
     * Erases all method calls from the cache and deletes the cache file
     */
    public void forget() {
        cache = new LinkedHashMap<>();
        cacheFile.delete();    }

    /**
     * Records a resource as being a dependency of the current method that is executing
     * @param resource the resource
     * @return the supplied resource
     * @param <T> the type of resource(s)
     */
    public <T extends Memorizable> T dependsOn(T resource) {
        dependencies.peek().add(resource);
        return resource;
    }

    /**
     * Creates a memoized instance of a build interface
     * @param <T> the build type
     * @param t the Java class of the build interface
     * @return the created build object
     */
    public <T> T instantiate(Class<T> t) {
        dependencies.push(new HashSet<>());
        return t.cast(Proxy.newProxyInstance(t.getClassLoader(), new Class[]{t}, this::_invokeMethod));
    }

    // This method's name starts with '_' to make it easy to filter out of stack traces
    private Object _invokeMethod(Object proxy, Method method, Object[] args)
            throws Throwable {
        Invocation signature = new Invocation(method, args);

        Status status = Status.COMPUTE;
        if (cache.containsKey(signature)) {
            Result result = cache.get(signature);

            if (!result.isCurrent()) {
                status = Status.UPDATE;
                cache.remove(signature);
            } else {
                listener.startMethod(Status.CURRENT, method, signature.params());
                dependencies.peek().addAll(result.sources());
                listener.endMethod(Status.CURRENT, method, signature.params(), result.value());
                return result.value();
            }
        }
        dependencies.push(new HashSet<>());
        listener.startMethod(status, method, signature.params());

        Object value = null;
        try {
            value = InvocationHandler.invokeDefault(proxy, method, args);
            if (value instanceof Serializable) {
                cache.put(signature, new Result(signature, value, dependencies.peek()));
            }
            return value;
        } finally {
            listener.endMethod(status, method, signature.params(), value);
            Set<Memorizable> used = dependencies.pop();
            dependencies.peek().addAll(used);
        }
    }
}
