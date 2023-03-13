package org.copalis.builder;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
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

    public void startMethod(Memorizer.Status status, Method method, List<Object> params) {
        print(status.toString().toLowerCase());
        print(":");
        print(" ".repeat(calls * 2 + 8 - status.toString().length()));
        print(method.getName());
        if (method.isVarArgs()) {
            for (int i = 0; i < params.size() - 1; i++) {
                printParam(params.get(i));
            }
            Object arr = params.get(params.size() - 1);
            if (Objects.nonNull(arr)) {
                for (int i = 0; i < Array.getLength(arr); i++) {
                    printParam(Array.get(arr, i));
                }
            }
        } else {
            for (Object param : params) {
                printParam(param);
            }
        }
        System.out.println();

        calls++;
    }

    private void printParam(Object param) {
        print(" ");
        if (param instanceof String) print("'");
        System.out.print(param);
        if (param instanceof String) print("'");
    }

    public void endMethod(Memorizer.Status status, Method method, List<Object> params, Object result) {
        calls--;
    }

    private void print(String str) {
        System.out.print(str);
    }

    public void execute(Function<T, ?> buildFn, Function<T, String> cacheDir, String[] args) {
        long start = System.currentTimeMillis();
        T obj = memo.instantiate(type);

        String cache = cacheDir.apply(obj) + '/' + CACHE_FILE;
        memo.setListener(this);

        LinkedList<String> params = new LinkedList<>(Arrays.asList(args));
        if ("-new".equals(params.peekFirst())) {
            params.removeFirst();
        } else {
            memo.loadCache(cache);
            memo.validateCache();
        }

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
        System.out.format("COMPLETED in %dms\n", System.currentTimeMillis() - start);
    }
}
