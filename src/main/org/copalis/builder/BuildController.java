package org.copalis.builder;

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
 * @author giles
 */
public class BuildController<T> implements Memorizer.Listener {
    private static final String CACHE_FILE = ".build-cache.ser";
    private static final boolean colors = Objects.nonNull(System.console());

    private static final String
    	RESET = 		"\033[0m",
    	RED_BRIGHT = 	"\033[0;91m",
		GREEN = 		"\033[0;32m",
    	GREEN_BRIGHT = 	"\033[0;92m",
		YELLOW = 		"\033[0;33m",
		CYAN = 			"\033[0;36m",
		WHITE_BOLD = 	"\033[1;37m";

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
        	switch (status) {
        	case CURRENT: color(GREEN); break;
        	case EXECUTE: color(YELLOW); break;
        	case UPDATE: color(CYAN); break;
        	}
            print("[").print(status.toString().toLowerCase());
            print(" ".repeat(7 - status.name().length()));
            print("] ");
            color(RESET);
            print(" ".repeat(calls * 2)).print(method.getName());
            for (Object param : params) {
                print(" ");
                if (param instanceof String) print("'");
                out.print(param);
                if (param instanceof String) print("'");
            }
            color(WHITE_BOLD);
            out.println();
        }

        calls++;
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

    private BuildController<T> print(String str) {
        out.print(str);
        return this;
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

        if (lastModified < cache.lastModified()) {
            memo.loadCache(cache.toString());
        } else if (cache.exists()) {
            System.out.println("Build script has changed, rebuilding all");
        }

        try {
        	try {
        		if (args.length == 0) {
        			buildFn.apply(obj);
        		} else {
        			for (String arg : args) {
        				type.getMethod(arg).invoke(obj);
        			}
        		}
        	} finally {
        		memo.saveCache(cache.toString());
        	}
        	color(GREEN_BRIGHT);
        	System.out.format("COMPLETED in %dms\n", System.currentTimeMillis() - start);
        } catch (InvocationTargetException e) {
        	e.getCause().printStackTrace();
        	color(RED_BRIGHT);
        	System.out.format("FAILED in %dms\n", System.currentTimeMillis() - start);
        } catch (Exception e) {
        	e.printStackTrace();
        	color(RED_BRIGHT);
        	System.out.format("FAILED in %dms\n", System.currentTimeMillis() - start);
        }
    }
}
