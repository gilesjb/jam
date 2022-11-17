package org.copalis.builder;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Controller<T> implements Memorizer.Listener {
    private static final String CACHE_FILE = ".build-cache.ser";

    private final Memorizer memo;
    private final Class<T> type;

    int indent = 0;

    public Controller(Memorizer memo, Class<T> type) {
        this.memo = memo;
        this.type = type;
    }

    public void starting(boolean cached, Method method, List<Object> params) {
        indent++;
    }

    public void completed(boolean cached, Method method, List<Object> params, Object result) {
        if (cached) {
        } else if (!type.isAssignableFrom(method.getDeclaringClass())) {
        } else if (!params.isEmpty()) {
            System.out.println(" ".repeat(indent) + method.getName()
            + params.stream().map(Object::toString).collect(Collectors.joining(",", "(", ")")) + " => "
            + result);
        } else {
            System.out.println(" ".repeat(indent) + method.getName() + " => " + result);
        }
        indent--;
    }

    public void execute(Function<T, ?> buildFn, Function<T, String> cacheDir, String[] args) {
        T obj = memo.instantiate(type);
        String cache = cacheDir.apply(obj) + '/' + CACHE_FILE;

        LinkedList<String> params = new LinkedList<>(Arrays.asList(args));
        if ("--all".equals(params.peekFirst())) {
            params.removeFirst();
        } else {
            memo.loadCache(cache);
            memo.validateCache();
        }

        memo.setListener(this);

        final Object result;
        if (!params.isEmpty()) {
            try {
                Method m = type.getMethod(params.removeFirst());
                result = m.invoke(obj);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            result = buildFn.apply(obj);
        }
        System.out.println("-> " + result);
        memo.saveCache(cache);
    }
}
