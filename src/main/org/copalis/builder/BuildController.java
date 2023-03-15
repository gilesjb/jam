package org.copalis.builder;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

public class BuildController<T> implements Memorizer.Listener {
    private static final String CACHE_FILE = ".build-cache.ser";

    record Call(Method method, List<Object> params) { }

    private final Memorizer memo;
    private final Class<T> type;
    private final Set<Call> cached = new HashSet<>();

    private int calls = 0;


    public BuildController(Memorizer memo, Class<T> type) {
        this.memo = memo;
        this.type = type;
    }

    public void startMethod(Memorizer.Status status, Method method, List<Object> params) {
        if (cached.add(new Call(method, params))) {
            print(status.toString().toLowerCase());
            print(":");
            print(" ".repeat(calls * 2 + 8 - status.toString().length()));
            print(method.getName());
            for (Object param : params) {
                print(" ");
                if (param instanceof String) print("'");
                System.out.print(param);
                if (param instanceof String) print("'");
            }
            System.out.println();
        }

        calls++;
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
        } else if ("-help".equals(params.peekFirst())) {
            System.out.println("Targets:");
            Stream.of(type.getMethods())
                .filter(m -> m.getParameterCount() == 0)
                .map(Method::getName)
                .forEach(System.out::println);
            return;
        } else {
            memo.loadCache(cache);
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
