
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.copalis.builder.Timestamped;

/**
 * Instantiates an interface using a dynamic proxy and memoizes the method calls.
 * Memoized results can be saved to a cache file and restored later.
 *
 * @author gilesjb
 */
public class Memorizer {
    private static final String CACHE_FILE = ".build-cache.ser";

    public interface Listener {
        default void starting(boolean cached, Method method, List<Object> params) { }
        default void completed(boolean cached, Method method, List<Object> params, Object result) { }
    }

    record Signature(String name, List<Object> params) implements Serializable {
        public Signature(Method method, Object[] params) {
            this(method.getName(), Objects.isNull(params) ? Collections.emptyList() : Arrays.asList(params));
        }
    }

    record Result(Object value, Map<String, Fileset> sources) implements Serializable { }

    private final LinkedList<Map<String, Fileset>> sourcefiles = new LinkedList<>();
    private Map<Signature, Result> cache = new HashMap<>();
    private Listener listener = new Listener() { };

    void setListener(Listener listener) {
        this.listener = listener;
    }

    void validateCache() {
        Map<Signature, Result> copy = new HashMap<>();
        cache.forEach((signature, result) -> {
            for (Object param : signature.params) {
                if (param instanceof Timestamped && !((Timestamped) param).isCurrent()) {
                    return;
                }
            }

            if (result.value() instanceof Timestamped && !((Timestamped) result.value()).isCurrent()) {
                return;
            }

            Map<String, Fileset> depended = result.sources();
            for (Map.Entry<String, Fileset> entry : depended.entrySet()) {
                Fileset files = Fileset.find(Path.of(""), entry.getKey());
                if (!files.equals(entry.getValue())) {
                    return;
                }
            }
            copy.put(signature, result);
        });

        cache = copy;
    }

    @SuppressWarnings("unchecked")
    void loadCache(String path) {
        try (FileInputStream file = new FileInputStream(new java.io.File(path, CACHE_FILE))) {
            try (ObjectInputStream obj = new ObjectInputStream(file)) {
                cache = (HashMap<Signature, Result>) obj.readObject();
            }
        } catch (FileNotFoundException e) {
        } catch (ClassNotFoundException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    void saveCache(String path) {
        try (FileOutputStream file = new FileOutputStream(new java.io.File(path, CACHE_FILE))) {
            try (ObjectOutputStream out = new ObjectOutputStream(file)) {
                out.writeObject(cache);
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
    Fileset dependsOn(String pattern, Fileset files) {
        sourcefiles.peek().put(pattern, files);
        return files;
    }

    <T, R> T instantiate(Class<T> t) {
        sourcefiles.push(new HashMap<>());

        T maker = t.cast(Proxy.newProxyInstance(t.getClassLoader(), new Class[]{t}, (proxy, method, args) -> {
            Signature signature = new Signature(method, args);

            if (cache.containsKey(signature)) {
                listener.starting(true, method, signature.params());
                Result result = cache.get(signature);
                sourcefiles.peek().putAll(result.sources());
                listener.completed(true, method, signature.params(), result.value());
                return result.value();
            } else {
                sourcefiles.push(new HashMap<>());
                listener.starting(false, method, signature.params());

                try {
                    Object value = InvocationHandler.invokeDefault(proxy, method, args);
                    Result result = new Result(value, sourcefiles.peek());
                    cache.put(signature, result);
                    listener.completed(false, method, signature.params(), result.value());
                    return value;
                } finally {
                    Map<String, Fileset> used = sourcefiles.pop();
                    sourcefiles.peek().putAll(used);
                }
            }
        }));
        return maker;
    }
}
