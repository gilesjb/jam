package org.copalis.builder;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public class BuildController<T> implements Memorizer.Listener {
    private static final String CACHE_FILE = ".build-cache.ser";

    private final Memorizer memo;
    private final Class<T> type;

    private final LinkedList<Set<String>> stack = new LinkedList<>();

    public BuildController(Memorizer memo, Class<T> type) {
        this.memo = memo;
        this.type = type;
        stack.push(new LinkedHashSet<>());
    }

    public void starting(boolean cached, Method method, List<Object> params) {
        if (type.isAssignableFrom(method.getDeclaringClass())) {
            stack.peek().add(method.getName() + (cached ? "*" : ""));
        }
        stack.push(new LinkedHashSet<>());
    }

    public void completed(boolean cached, Method method, List<Object> params, Object result) {
        if (!cached && type.isAssignableFrom(method.getDeclaringClass())) {
            System.out.println(method.getName() + ':');
            printExecution(params, result);
        }
        stack.pop();
    }

    private void printExecution(List<Object> params, Object result) {
        for (Object param : params) {
            System.out.println("    < " + param);
        }
        for (Object other : stack.peek()) {
            System.out.println("    [" + other + "]");
        }
        if (result instanceof Iterable<?> iter) {
            for (Iterator<?> i = iter.iterator(); i.hasNext(); ) {
                System.out.println("    > " + i.next());
            }
        } else if (Objects.nonNull(result)) {
            System.out.println("    > " + result);
        }
    }

    public void execute(Function<T, ?> buildFn, Function<T, String> cacheDir, String[] args) {
        T obj = memo.instantiate(type);
        String cache = cacheDir.apply(obj) + '/' + CACHE_FILE;

        LinkedList<String> params = new LinkedList<>(Arrays.asList(args));
        if ("-new".equals(params.peekFirst())) {
            params.removeFirst();
        } else {
            memo.loadCache(cache);
            memo.validateCache();
        }

        memo.setListener(this);

        if (!params.isEmpty()) {
            try {
                Method m = type.getMethod(params.removeFirst());
                m.invoke(obj);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            Object result = buildFn.apply(obj);
            System.out.println("default-build:");
            printExecution(Collections.emptyList(), result);
        }
        memo.saveCache(cache);
    }
}
