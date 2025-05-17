package org.copalis.jam;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.copalis.jam.Memorizer.Invocation;
import org.copalis.jam.Memorizer.Result;

/**
 * Controller for the build process
 * @param <T> the build class
 *
 * @author gilesjb
 */
public class BuildController<T> implements Memorizer.Observer {
    /**
     * A dummy memoizer that is replaced by the real memoizer during execution of projects
     */
    public static final Memorizer MEMO = new Memorizer() {
        @Override public String toString() {
            return Memorizer.class.getSimpleName();
        }
    };

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
    private T object;

    /**
     * Creates a build controller instance
     * @param type the build class
     */
    public BuildController(Class<T> type) {
        this.type = type;
        this.memo = new Memorizer(this, new File("." + type.getSimpleName() + ".ser"));
    }

    public void startMethod(Memorizer.Status status, Method method, List<Object> params) {
        if (status != Memorizer.Status.CURRENT || cached.add(new Call(method, params))) {
            switch (status) {
            case CURRENT: color(GREEN); break;
            case COMPUTE: color(YELLOW); break;
            case REFRESH: color(CYAN); break;
            }
            print("[").print(status.name().toLowerCase());
            print(" ".repeat(7 - status.name().length()));
            print("] ");
            color(RESET).printMethod(method.getName(), params);
            line();
        }

        calls++;
    }

    public Object endMethod(Memorizer.Status status, Method method, List<Object> params, Object result) {
        calls--;
        if (result == MEMO) {
            return memo;
        } else {
            return result;
        }
    }

    /**
     * Runs the build
     * @param buildFn a function that invokes the default build target
     * @param args the build's command line arguments
     */
    public void execute(Function<T, ?> buildFn, String[] args) {
        long start = System.currentTimeMillis();
        boolean exit = false;

        try {
            for (int opt = 0; opt < args.length && args[opt].startsWith("-"); opt++) {
                color(BOLD);
                switch (args[opt]) {
                case "--cache":
                    load();
                    printCacheContents();
                    break;
                case "--targets":
                    load();
                    printBuildTargets(buildFn);
                    break;
                default:
                    color(RED_BRIGHT).print("Illegal option: ").print(args[opt]).color(BOLD).line();
                case "--help":
                    print("Jam build tool").line();
                    print("Options ").line();
                    print("    --help             display this help message").line();
                    print("    --targets          display the available build targets").line();
                    print("    --cache            display the cache contents").line();
                    print("    <target-name>...   builds the specified targets").line();
                    print("Running the build with no arguments builds the default target").line();
                }
                exit = true;
            }

            if (!exit) {
                try {
                    if (args.length == 0) {
                        printResult(buildFn.apply(load()));
                    } else {
                        for (String arg : args) {
                            printResult(type.getMethod(arg).invoke(load()));
                        }
                    }
                } finally {
                    if (memo.entries().findAny().isPresent()) {
                        memo.save();
                    }
                }
                color(GREEN_BRIGHT).print("COMPLETED");
            }
        } catch (InvocationTargetException | UndeclaredThrowableException e) {
            printStackTrace(e.getCause());
            color(RED_BRIGHT).print("FAILED");
        } catch (Exception e) {
            printStackTrace(e);
            color(RED_BRIGHT).print("FAILED");
        } finally {
            if (!exit) print(String.format(" in %dms", System.currentTimeMillis() - start)).line();
        }
    }

    private T load() throws ClassNotFoundException, IOException {
        if (Objects.isNull(object)) {
            object = memo.instantiate(type);

            URL scriptLocation = type.getProtectionDomain().getCodeSource().getLocation();
            long scriptModified = Objects.nonNull(scriptLocation)
                    ? new File(scriptLocation.getPath()).lastModified() : Long.MIN_VALUE;

            if (memo.cacheFile().exists() && memo.cacheFile().lastModified() < scriptModified) {
                color(CYAN_BRIGHT).print("Build script has been modified; Using new method cache.").color(RESET).line();
            } else if (memo.cacheFile().exists()) {
                memo.loadCache();
            }
        }

        return object;
    }

    private void printCacheContents() {
        print("Contents of cache file ").print(memo.cacheFile()).line();
        memo.entries().forEach(e -> {
                printResultStatus(e);
                color(BOLD).printMethod(e.signature().name(), e.signature().params());
                color(RESET).print(" = ").print(e.value());
                line();
            });
    }

    private void printBuildTargets(Function<T, ?> buildFn) {
        color(RESET).printTargets(type, new HashSet<>());

        Object proxy = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (p, m, a) -> {
            throw new UnsupportedOperationException(m.getName());
        });

        try {
            if (Objects.nonNull(buildFn)) buildFn.apply(type.cast(proxy));
        } catch (UnsupportedOperationException e) {
            print("Default target is: ").color(BOLD).print(e.getMessage()).line();
        }
    }

    private void printTargets(Class<?> t, Set<String> visited) {
        List<Method> targets = Stream.of(t.getDeclaredMethods())
                .filter(Method::isDefault)
                .filter(m -> m.getParameterCount() == 0 && !m.isSynthetic())
                .filter(m -> !visited.contains(m.getName()))
                .collect(Collectors.toList());

        if (!targets.isEmpty()) {
            color(BOLD).print(t.getSimpleName()).color(RESET).line();
            for (Method m : targets) {
                printResultStatus(memo.lookup(new Invocation(m)));
                print(m.getName()).print(" : ");
                print(m.getReturnType().getSimpleName()).line();
                visited.add(m.getName());
            }
        }

        Class<?>[] interfaces = t.getInterfaces();
        for (int i = interfaces.length - 1; i >= 0; i--) {
            printTargets(interfaces[i], visited);
        }
    }

    private void printResultStatus(Result result) {
        if (Objects.isNull(result)) {
            print("         ");
        } else if (result.isCurrent()) {
            color(GREEN).print("[fresh]  ");
        } else {
            color(CYAN).print("[stale]  ");
        }
        color(RESET);
    }

    private void printResult(Object result) {
        if (Objects.nonNull(result)) {
            color(BOLD).print("Result: ").printValue(result);
            line();
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
                        || el.getClassName().equals(Memorizer.class.getName())
                        || Objects.isNull(el.getFileName())) {
                    continue;
                }
                System.err.println("\tat " + el);
            }
        }
    }

    private void printMethod(String method, List<Object> params) {
        print(" ".repeat(calls * 2)).print(method);
        for (Object param : params) {
            print(" ");
            printValue(param);
        }
    }

    private void  printValue(Object val) {
        if (val instanceof String) print("'");
        out.print(val);
        if (val instanceof String) print("'");
    }

    private BuildController<T> color(String str) {
        if (colors) {
            out.print(str);
        }
        return this;
    }

    private BuildController<T> print(Object obj) {
        out.print(Objects.toString(obj));
        return this;
    }

    private void line() {
        out.println();
    }
}
