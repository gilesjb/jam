package org.copalis.jam.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.copalis.jam.memo.Invocation;
import org.copalis.jam.memo.Memorizer;
import org.copalis.jam.memo.Observer;
import org.copalis.jam.memo.Result;

/**
 * A build process command-line argument parser and controller.
 *
 * The controller is constructed with a reference to a build interface,
 * which defines the build targets and logic:
 * The controller treats each 0-argument method declared by the build interface as a build target.
 * For example, the method
 * <pre>
 * {@code default File jarFile() { ... }}
 * </pre>
 * defines a build target called {@code jarFile}.
 * <p>
 * When the controller is constructed it creates a memoized instance of the build interface
 * and registers itself as an observer so that it can intercept and log all
 * the method calls.
 * <p>
 * The state of the memoizer's cache is saved to a file called {@code .<project>.ser},
 * where {@code <project>} is the unqualified name of the build interface.
 * This saved cache tracks the state of source files and ensures that derived artifacts
 * are rebuilt when the sources change.
 *
 * @param <T> the build interface type
 * @author gilesjb
 */
public class BuildController<T> {
    /**
     * A dummy memoizer that is replaced by the real memoizer during execution of projects
     */
    public static final Memorizer MEMO = new Memorizer() {
        @Override public String toString() {
            return Memorizer.class.getSimpleName();
        }
    };

    /**
     * A dummy File object that is replaced by a reference to the memoizer's cache file
     */
    public static final File CACHE_FILE = new File("*cache*");

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

    private final File cacheFile;
    private final Memorizer memo;
    private final Class<T> type;
    private final Set<Call> cached = new HashSet<>();
    private final Map<Object, Object> injected = new IdentityHashMap<>();
    private final PrintStream out = System.out;

    private final Observer observer = new Observer() {
        public void startMethod(Observer.Status status, Method method, List<Object> params) {
            if (status != Observer.Status.CURRENT || cached.add(new Call(method, params))) {
                switch (status) {
                case CURRENT: color(GREEN); break;
                case COMPUTE: color(YELLOW); break;
                case REFRESH: color(CYAN); break;
                }
                print("[").print(status.name().toLowerCase());
                print(" ".repeat(7 - status.name().length()));
                print("]  ");
                color(RESET).printMethod(method.getName(), params);
                line();
            }

            calls++;
        }

        public Object endMethod(Observer.Status status, Method method, List<Object> params, Object result) {
            calls--;
            lastResult = injected.getOrDefault(result, result);
            return lastResult;
        }
    };

    private int calls = 0;
    private T object;
    private Object lastResult;

    /**
     * Creates a build controller instance
     * @param type the build class
     */
    public BuildController(Class<T> type) {
        this.type = type;
        this.cacheFile = new File("." + type.getSimpleName() + ".ser");
        this.memo = new Memorizer(observer);
        injected.put(MEMO, memo);
        injected.put(CACHE_FILE, cacheFile);
    }

    /**
     * Executes a build as specified by command-line arguments.
     * <p>
     * Depending on the command-line arguments supplied to this method,
     * either target methods on the build interface are executed
     * or information about the build or its status is displayed to the console.
     * <p>
     * Command-line arguments may be
     * <dl>
     * <dt>{@code --help}<dd>Displays help information
     * <dt>{@code --cache}<dd>Displays the contents of the memoization cache
     * <dt>{@code --targets}<dd>Displays the names, return types, and cache status of the target methods
     * <dt><i>target-name</i><dd>Executes the target method with the specified name
     * </dl>
     * If no arguments are specified, {@code buildFn} is invoked.
     *
     * @param buildFn a consumer that invokes the default build target
     * @param args the build's command line arguments.
     */
    public void executeBuild(Consumer<T> buildFn, String[] args) {
        long start = System.currentTimeMillis();
        String script = ProcessHandle.current().info().arguments()
                .map(a -> a[a.length - args.length - 1]).orElse("");
        boolean exit = false;

        try {
            for (int opt = 0; opt < args.length && args[opt].startsWith("-"); opt++) {
                switch (args[opt]) {
                case "--cache":
                    load(script);
                    printCacheContents();
                    break;
                case "--targets":
                    load(script);
                    printBuildTargets(buildFn);
                    break;
                default:
                    color(RED_BRIGHT).print("Illegal option: ").color(RESET).print(args[opt]).line();
                case "--help":
                    String path = "   " + (new File(script).exists() ? script : "<script>");
                    print("Usage:").line();
                    print(path).print("              Build the default target").line();
                    print(path).print(" <targets>    Build specified targets").line();
                    print(path).print(" --targets    Print available build targets").line();
                    print(path).print(" --cache      Print cache contents").line();
                    print(path).print(" --help       Print this help message").line();
                }
                exit = true;
            }

            if (!exit) {
                try {
                    if (args.length == 0) {
                        buildFn.accept(load(script));
                    } else {
                        for (String arg : args) {
                            type.getMethod(arg).invoke(load(script));
                        }
                    }
                    if (Objects.nonNull(lastResult)) {
                        color(BOLD).print("Result: ").printValue(lastResult);
                        line();
                    }

                } finally {
                    if (memo.entries().findAny().isPresent()) {
                        try (OutputStream out = new FileOutputStream(cacheFile)) {
                            memo.save(out);
                        }
                    } else if (cacheFile.exists()) {
                        cacheFile.delete();
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
            if (!exit) print(String.format(" in %dms", System.currentTimeMillis() - start)).color(RESET).line();
        }
    }

    private T load(String script) throws ClassNotFoundException, IOException {
        if (Objects.isNull(object)) {
            object = memo.instantiate(type);
            File scriptFile = new File(script);

            if (cacheFile.exists() && cacheFile.lastModified() < scriptFile.lastModified()) {
                color(CYAN_BRIGHT).print("Build script has been modified; Using new method cache.").color(RESET).line();
            } else if (cacheFile.exists()) {
                try (InputStream in = new FileInputStream(cacheFile)) {
                    memo.load(in);
                }
            }
        }

        return object;
    }

    private void printCacheContents() {
        print("Contents of cache file ").print(cacheFile).line();
        memo.entries().forEach(e -> {
                printResultStatus(e);
                color(BOLD).printMethod(e.signature().name(), e.signature().params());
                color(RESET).print(" = ").printValue(e.value());
                line();
            });
    }

    private void printBuildTargets(Consumer<T> buildFn) {
        color(RESET).printTargets(type, new HashSet<>());
        try {
            buildFn.accept(
                    type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (p, m, a) -> {
                        print("Default target: ").color(BOLD).print(m.getName()).line();
                        return null;
                    })));
        } catch (NullPointerException e) { } // thrown if buildFn has primitive return type
    }

    private void printTargets(Class<?> t, Set<String> visited) {
        List<Method> targets = Stream.of(t.getDeclaredMethods())
                .filter(Method::isDefault)
                .filter(m -> m.getParameterCount() == 0 && !m.isSynthetic())
                .filter(m -> !visited.contains(m.getName()))
                .collect(Collectors.toList());

        if (!targets.isEmpty()) {
            print(t.getSimpleName() + " targets").line();
            for (Method m : targets) {
                printResultStatus(memo.lookup(new Invocation(m)));
                color(BOLD).print(m.getName()).color(RESET).print(" : ");
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
        } else if (result.modified()) {
            color(CYAN).print("[stale]  ");
        } else {
            color(GREEN).print("[fresh]  ");
        }
        color(RESET);
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
                if (!el.getFileName().endsWith(".java")) break;
            }
        }
    }

    private void printMethod(String method, List<Object> params) {
        print("  ".repeat(calls)).print(method);
        for (Object param : params) {
            print(" ");
            printValue(param);
        }
    }

    private void printValue(Object val) {
        if (val instanceof String) print("'");
        String str = val.toString();
        if (str.length() > 200) {
            str = str.substring(0, 200) + "...";
        }
        int nl = str.indexOf('\n');
        if (nl > 10) {
            str = str.substring(0, nl) + "...";
        }
        out.print(str);
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
