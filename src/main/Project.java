import java.util.function.Consumer;
import java.util.function.Function;

import org.copalis.jam.BuildController;

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
    default <T> T get(T obj) {
        return obj;
    }

    /**
     * Empties the result cache
     */
    default void clean() {
        get(BuildController.MEMO).forget();
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
