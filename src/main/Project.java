import java.io.File;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.copalis.jam.cli.BuildController;

/**
 * The base Project interface which defines the {@code clean} build target
 * and provides functionality to track source file dependencies,
 * control the build process and handle command-line options.
 *
 * @see BuildController#executeBuild(Consumer, String[])
 * @author gilesjb
 */
public interface Project {

    /**
     * Wraps a value in a non-serializable {@link Supplier}.
     * A method can be made non-cacheable by returning an object of this type.
     * @param <T> the type of value
     * @param val an arbitrary value
     * @return a supplier containing val
     */
    default <T> Supplier<T> currently(T val) {
        return () -> val;
    }

    /**
     * Returns the value passed to it
     * so that the build controller can perform return value replacement on the value.
     * @param <T> the type of value
     * @param val an arbitrary value
     * @return val
     */
    default <T> T using(T val) {
        return val;
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
