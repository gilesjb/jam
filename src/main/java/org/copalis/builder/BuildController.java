package org.copalis.builder;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

public class BuildController<T> implements Memorizer.Listener {
    private static final String CACHE_FILE = ".build-cache.ser";

    private final Memorizer memo;
    private final Class<T> type;

    private int calls = 0;

    public BuildController(Memorizer memo, Class<T> type) {
        this.memo = memo;
        this.type = type;
    }

    public void starting(boolean cached, Method method, List<Object> params) {
        if (!cached) {
            printMethodCall(false, method, params);
        }
        calls++;
    }

    public void completed(boolean cached, Method method, List<Object> params, Object result) {
        calls--;
        if (cached) {
            printMethodCall(true, method, params);
        }
    }

    private void printMethodCall(boolean cached, Method method, List<Object> params) {
        print(cached ? "cached:" : "call:  ");
        print("  ".repeat(calls));
        print(method.getName());
        for (Object param : params) {
            print(" ");
            if (param instanceof String) print("'");
            System.out.print(param);
            if (param instanceof String) print("'");
        }
        System.out.println();
    }

    private void print(String str) {
        System.out.print(str);
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
            buildFn.apply(obj);
        }
        memo.saveCache(cache);
    }
}
