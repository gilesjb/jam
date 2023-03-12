package org.copalis.builder;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Instantiates an interface using a dynamic proxy and memoizes the method calls.
 * Memoized results can be saved to a cache file and restored later.
 *
 * @author gilesjb
 */
public class Memorizer {

    public enum Status {
        CREATE, UPDATE, CURRENT
    }

    public interface Listener {
        default void startMethod(Status status, Method method, List<Object> params) { }
        default void endMethod(Status status, Method method, List<Object> params, Object result) { }
    }

    record Signature(String name, List<Object> params) implements Serializable {
        public Signature(Method method, Object[] params) {
            this(method.getName(), Objects.isNull(params) ? Collections.emptyList() : Arrays.asList(params));
        }
    }

    record Result(Object value, Set<Checked> sources) implements Serializable { }

    static final Result STALE = new Result(null, Collections.emptySet());

    private final LinkedList<Set<Checked>> dependencies = new LinkedList<>();
    private Map<Signature, Result> cache = new HashMap<>();
    private Listener listener = new Listener() { };

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void validateCache() {
        Map<Signature, Result> copy = new HashMap<>();
        cache.forEach((signature, result) -> {
            copy.put(signature, STALE);

            for (Object param : signature.params) {
                if (param instanceof Checked checked && !checked.isCurrent()) {
                    return;
                }
            }

            if (result.value() instanceof Checked checked && !checked.isCurrent()) {
                return;
            }

            for (Checked depended : result.sources) {
                if (!depended.isCurrent()) {
                    return;
                }
            }

            copy.put(signature, result);
        });

        cache = copy;
    }

    @SuppressWarnings("unchecked")
    public void loadCache(String path) {
        try (FileInputStream file = new FileInputStream(path)) {
            try (ObjectInputStream obj = new ObjectInputStream(file)) {
                cache = (HashMap<Signature, Result>) obj.readObject();
            }
        } catch (FileNotFoundException | InvalidClassException e) {
        } catch (ClassNotFoundException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveCache(String path) {
        Map<Signature, Result> save = cache.entrySet().stream()
                .filter(e -> e.getValue().value() instanceof Serializable)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        try (FileOutputStream file = new FileOutputStream(path)) {
            try (ObjectOutputStream out = new ObjectOutputStream(file)) {
                out.writeObject(save);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Records files as being a dependency of the current method that is executing
     * @param pattern
     * @param files
     * @return the supplied files
     */
    public <T extends Checked> T dependsOn(T files) {
        dependencies.peek().add(files);
        return files;
    }

    public <T, R> T instantiate(Class<T> t) {
        dependencies.push(new HashSet<>());
        return t.cast(Proxy.newProxyInstance(t.getClassLoader(), new Class[]{t}, this::invokeMethod));
    }

    private Object invokeMethod(Object proxy, Method method, Object[] args)
            throws Throwable {
        Signature signature = new Signature(method, args);

        Status status = Status.CREATE;
        if (cache.containsKey(signature)) {
            Result result = cache.get(signature);

            if (result == STALE) {
                status = Status.UPDATE;
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
            cache.put(signature, new Result(value, dependencies.peek()));
            return value;
        } finally {
            listener.endMethod(status, method, signature.params(), value);
            Set<Checked> used = dependencies.pop();
            dependencies.peek().addAll(used);
        }
    }
}
