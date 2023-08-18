package org.copalis.jam;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Controller for the build process
 * @param <T> the build class
 *
 * @author gilesjb
 */
public class BuildController<T> implements Memorizer.Listener {
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

        if (options(buildFn, args) > 0) return;

        try {
            URL scriptLocation = type.getProtectionDomain().getCodeSource().getLocation();
            long scriptModified = Objects.nonNull(scriptLocation)
                    ? new File(scriptLocation.getPath()).lastModified() : Long.MIN_VALUE;

            T obj = memo.instantiate(type);
            File cache = new File(cacheDir.apply(obj) + "/." + type.getSimpleName() + ".ser");

            memo.resetCache();
            memo.setListener(this);

            if (cache.exists() && cache.lastModified() < scriptModified) {
                color(CYAN_BRIGHT).print("Build script has changed, rebuilding all").line();
            } else if (cache.exists()) {
                memo.loadCache(cache);
            }

            try {
                if (args.length == 0) {
                    printResult(buildFn.apply(obj));
                } else {
                    for (String arg : args) {
                        printResult(type.getMethod(arg).invoke(obj));
                    }
                }
            } finally {
                if (cache.getParentFile().exists()) {
                    memo.saveCache(cache);
                }
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

    private int options(Function<T, ?> buildFn, String[] args) {
        int ix = 0;

        for (; ix < args.length && args[ix].startsWith("-"); ix++) {
            color(BOLD);
            switch (args[ix]) {
            case "--targets":
                printBuildTargets(buildFn);
                break;
            default:
                color(RED_BRIGHT).print("Illegal option: ").print(args[ix]).color(BOLD).line();
            case "--help":
                print("Options ").line();
                print("    --help             display this help message").line();
                print("    --targets          display the available build targets").line();
                print("    <target-name>...   builds the specified targets, or the default").line();
            }
        }
        return ix;
    }

    private void printBuildTargets(Function<T, ?> buildFn) {
        T proxy = type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (p, m, a) -> {
            throw new UnsupportedOperationException(m.getName());
        }));

        try {
            if (Objects.nonNull(buildFn)) buildFn.apply(proxy);
            printBuildTargets(proxy, null);
        } catch (UnsupportedOperationException e) {
            printBuildTargets(proxy, e.getMessage());
        }
    }

    private void printBuildTargets(T proxy, String target) {
        for (Class<?> iface : proxy.getClass().getInterfaces()) {
            print(iface.getSimpleName()).print(" targets").line();
            Stream.of(iface.getMethods())
                    .filter(m -> m.getParameterCount() == 0)
                    .forEach(m -> print("       ")
                            .print(Objects.equals(target, m.getName()) ? "*" : " ")
                            .print(m.getName()).print(" : ")
                            .print(m.getReturnType().getSimpleName()).line());
        }
    }

    private void printResult(Object result) {
        if (Objects.nonNull(result)) {
            color(BOLD).print("Result: ").printValue(result).line();
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
                        || Objects.isNull(el.getFileName())) {
                    continue;
                }
                System.err.println("\tat " + el);
            }
        }
    }
}
