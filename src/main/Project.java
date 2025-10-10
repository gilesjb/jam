import java.io.File;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import org.copalis.jam.cli.BuildController;
import org.copalis.jam.memo.Memorizer;
import org.copalis.jam.memo.Mutable;

/**
 * A base Project interface which defines the {@code clean} build target
 * and provides functionality to track source file dependencies,
 * control the build process and handle command-line options.
 *
 * @see BuildController#executeBuild(Consumer, String[])
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
     * Records a mutable resource such as a {@link File} as a dependency. If the resource changes,
     * the cached results of all dependent methods will invalidated and build targets
     * that depend on it will be marked as {@code [stale]}.
     * <p>
     * The set of methods that will be recorded as dependent on the resource is specified by
     * {@link Memorizer#dependsOn(Mutable)}.
     *
     * @param <T> the resource type
     * @param resource a reference to a mutable resource
     * @return the resource
     * @see Memorizer#dependsOn(Mutable)
     */
    default <T extends Mutable> T dependsOn(T resource) {
        using(BuildController.MEMO).dependsOn(resource);
        return resource;
    }

    /**
     * The {@code clean} build target.
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
     * Executes a project using {@link BuildController#executeBuild(Consumer, String[])}
     * @param <T> the project type
     * @param t the project interface reference
     * @param fn a consumer that calls the default build method
     * @param args command line arguments
     */
    public static <T> void run(Class<T> t, Consumer<T> fn, String[] args) {
        new BuildController<>(t).executeBuild(fn, args);
    }
}
