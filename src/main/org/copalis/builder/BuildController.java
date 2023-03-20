package org.copalis.builder;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Controller for the build process
 * @param <T> the build class
 *
 * @author giles
 */
public class BuildController<T> implements Memorizer.Listener {
    private static final String CACHE_FILE = ".build-cache.ser";

    record Call(Method method, List<Object> params) { }

    private final Memorizer memo;
    private final Class<T> type;
    private final Set<Call> cached = new HashSet<>();
    private final PrintStream out = System.out;

    private int calls = 0;

    /**
     * Creates a build controller instance
     * @param memo a memorizer
     * @param type the build class
     */
    public BuildController(Memorizer memo, Class<T> type) {
        this.memo = memo;
        this.type = type;
    }

    public void startMethod(Memorizer.Status status, Method method, List<Object> params) {
        if (cached.add(new Call(method, params))) {
            print("[");
            print(status.toString().toLowerCase());
            print(" ".repeat(7 - status.name().length()));
            print("] ");
            print(" ".repeat(calls * 2));
            print(method.getName());
            for (Object param : params) {
                print(" ");
                if (param instanceof String) print("'");
                out.print(param);
                if (param instanceof String) print("'");
            }
            out.println();
        }

        calls++;
    }

    public void endMethod(Memorizer.Status status, Method method, List<Object> params, Object result) {
        calls--;
    }

    private void print(String str) {
        out.print(str);
    }

    /**
     * Runs the build
     * @param buildFn a function representing the default build target
     * @param cacheDir a function that returns the path of the directory to put the build cache in
     * @param args the build's command line arguments
     */
    public void execute(Function<T, ?> buildFn, Function<T, String> cacheDir, String[] args) {
        long start = System.currentTimeMillis();

        URL scriptLocation = type.getProtectionDomain().getCodeSource().getLocation();
        long lastModified = Objects.isNull(scriptLocation) ? Long.MIN_VALUE
                : new File(scriptLocation.getPath()).lastModified();

        T obj = memo.instantiate(type);
        File cache = new File(cacheDir.apply(obj) + '/' + CACHE_FILE);

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
            if (lastModified < cache.lastModified()) {
                memo.loadCache(cache.toString());
            } else if (cache.exists()) {
                System.out.println("Build script has changed, rebuilding all");
            }
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
        memo.saveCache(cache.toString());
        System.out.format("COMPLETED in %dms\n", System.currentTimeMillis() - start);
    }
}
