package org.copalis.jam;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Controller for the build process
 * @param <T> the build class
 *
 * @author gilesjb
 */
public class BuildController<T> implements Memorizer.Listener {
    private static final String CACHE_FILE = ".jam-cache";
    private static final boolean colors = Objects.nonNull(System.console());

    private static final String
        RESET =         "\033[0m",
        BOLD =          "\033[0;1m",
        RED_BRIGHT =    "\033[0;91m",
        GREEN =         "\033[0;32m",
        GREEN_BRIGHT =  "\033[0;92m",
        YELLOW =        "\033[0;33m",
        CYAN =          "\033[0;36m",
        CYAN_BRIGHT =   "\033[0;96m";

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
        if (status != Memorizer.Status.CURRENT || cached.add(new Call(method, params))) {
            switch (status) {
            case CURRENT: color(GREEN); break;
            case EXECUTE: color(YELLOW); break;
            case UPDATE: color(CYAN); break;
            }
            print("[").print(status.name().toLowerCase());
            print(" ".repeat(7 - status.name().length()));
            print("] ");
            color(RESET);
            print(" ".repeat(calls * 2)).print(method.getName());
            for (Object param : params) {
                print(" ");
                printValue(param);
            }
            color(BOLD).line();
        }

        calls++;
    }

    private BuildController<T>  printValue(Object val) {
        if (val instanceof String) print("'");
        out.print(val);
        if (val instanceof String) print("'");
        return this;
    }

    public void endMethod(Memorizer.Status status, Method method, List<Object> params, Object result) {
        calls--;
    }

    private BuildController<T> color(String str) {
        if (colors) {
            out.print(str);
        }
        return this;
    }

    private BuildController<T> print(Object obj) {
        out.print(obj.toString());
        return this;
    }

    private void line() {
        out.println();
    }

    /**
     * Runs the build
     * @param buildFn a function representing the default build target
     * @param cacheDir a function that returns the path of the directory to put the build cache in
     * @param args the build's command line arguments
     */
    public void execute(Function<T, ?> buildFn, Function<T, String> cacheDir, String[] args) {
        long start = System.currentTimeMillis();

        try {
            URL scriptLocation = type.getProtectionDomain().getCodeSource().getLocation();
            long scriptModified = Objects.nonNull(scriptLocation)
                    ? new File(scriptLocation.getPath()).lastModified() : Long.MIN_VALUE;

            T obj = memo.instantiate(type);
            File cache = new File(cacheDir.apply(obj) + '/' + CACHE_FILE);

            memo.resetCache();
            memo.setListener(this);

            if (cache.exists() && cache.lastModified() < scriptModified) {
                color(CYAN_BRIGHT).print("Build script has changed, rebuilding all").line();
            } else if (cache.exists()) {
                memo.loadCache(cache);
            }

            Object result = null;
            try {
                if (args.length == 0) {
                    result = buildFn.apply(obj);
                } else {
                    for (String arg : args) {
                        result = type.getMethod(arg).invoke(obj);
                    }
                }
            } finally {
                if (cache.getParentFile().exists()) {
                    memo.saveCache(cache);
                }
            }
            if (Objects.nonNull(result)) {
                print("Result: ").printValue(result).line();

            }
            color(GREEN_BRIGHT).print("COMPLETED");
        } catch (InvocationTargetException e) {
            printStackTrace(e.getCause());
            color(RED_BRIGHT).print("FAILED");
        } catch (Exception e) {
            printStackTrace(e);
            color(RED_BRIGHT).print("FAILED");
        } finally {
            print(String.format(" in %dms", System.currentTimeMillis() - start)).line();
        }
    }

    /**
     * Print the stack trace for an exception, with reflection methods filtered out
     * @param ex the exception
     */
    private void printStackTrace(Throwable ex) {
        synchronized(System.err) {
            color(RED_BRIGHT);
            System.err.println(ex);
            color(RESET);
            for (StackTraceElement el : ex.getStackTrace()) {
                if (el.getClassName().contains(".reflect.")
                        || el.getMethodName().startsWith("_")
                        || el.getFileName() == null) {
                    continue;
                }
                System.err.println("\tat " + el);
            }
        }
    }
}
