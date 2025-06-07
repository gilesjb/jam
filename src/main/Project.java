import java.io.File;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import org.copalis.jam.BuildController;
import org.copalis.jam.Mutable;

/**
 * A build project that provides common file manipulation methods
 *
 * @author gilesjb
 */
public interface Project {

    /**
     * Reflects a value so that build controller can override the value
     * @param <T> the type of object
     * @param obj the value to return
     * @return the same value passed to it, which may be replaced by the controller
     */
    default <T> T using(T obj) {
        return obj;
    }

    /**
     * Records a dependency on a mutable resource.
     * The resource will be recorded as a dependency of all project methods currently on the call stack,
     * and all methods it is passed to.
     * @param <T> the resource type
     * @param resource a reference to a mutable resource
     * @return the
     */
    default <T extends Mutable> T dependsOn(T resource) {
        using(BuildController.MEMO).dependsOn(resource);
        return resource;
    }

    /**
     * Empties the result cache and deletes the cache file, if it exists
     */
    default void clean() {
        File cacheFile = using(BuildController.CACHE_FILE);
        if (Objects.nonNull(cacheFile) && cacheFile.exists()) {
            cacheFile.delete();
        }
        using(BuildController.MEMO).forget();
    }


    /**
     * Executes a project
     * @param <T> the project type
     * @param t the project class reference
     * @param fn a consumer that calls the default build method
     * @param args command line arguments
     */
    public static <T extends Project> void run(Class<T> t, Consumer<T> fn, String[] args) {
        new BuildController<>(t).execute(o -> {
            fn.accept(o);
            return null;
        }, args);
    }

    /**
     * Executes a project
     * @param <T> The project type
     * @param t the project class reference
     * @param fn a function that calls the default build method
     * @param args command line arguments
     */
    public static <T extends Project> void run(Class<T> t, Function<T, ?> fn, String[] args) {
        new BuildController<>(t).execute(fn, args);
    }
}
