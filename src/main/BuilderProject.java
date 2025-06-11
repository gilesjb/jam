import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.copalis.jam.util.Cmd;
import org.copalis.jam.util.Paths;

/**
 * A base Project for scripts that create build artifacts derived from source files.
 * All source files should be located inside the directory specified by the {@link #sourcePath()}
 * build target, and build artifacts are written to the directory specified by {@link #buildPath()}.
 * <p>
 * To ensure that dependencies on source files are correctly tracked,
 * get references to source files using {@link #sourceFile(String)} and {@link #sourceFiles(String)}.
 *
 * @author gilesjb
 */
public interface BuilderProject extends Project {

    /**
     * Gets the root directory for the project's source code.
     * The default source code directory is {@code src};
     * Override this method to specify a different location.
     * @return the path of the source code directory
     */
    default String sourcePath() {
        return "src";
    }

    /**
     * Gets the root directory for the project's build artifacts.
     * The default build artifact directory is {@code build};
     * Override this method to specify a different location.
     * @return the path of the build directory
     */
    default String buildPath() {
        return "build";
    }

    /**
     * Generates the path of a build directory and creates the directory if it does not exist
     * @param path a path relative to {@link #buildPath()}
     * @return the directory path
     */
    default String buildPath(String path) {
        Path p = Path.of(buildPath(), path);
        try {
            Files.createDirectories(p);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return p.toString();
    }

    /**
     * Deletes the build directory specified by {@link #buildPath()}.
     * Also: {@inheritDoc}
     */
    default void clean() {
        rmdir(buildPath());
        Project.super.clean();
    }

    /**
     * Deletes a directory, which must be the build directory or one of its children
     * @param path the directory path
     */
    default void rmdir(String path) {
        Path p = Path.of(path);
        if (!p.startsWith(Path.of(buildPath()))) {
            throw new IllegalArgumentException(path + " is not a build directory");
        }
        Paths.rmDir(p);
    }

    /**
     * Gets source files matching a pattern.
     * The matching files will be recorded as dependencies of all methods on the call stack.
     * @param pattern a glob pattern to search files within {@link #sourcePath()}
     * @return a fileset of the matching files
     */
    default Fileset sourceFiles(String pattern) {
        return dependsOn(Fileset.find(sourcePath() + '/' + pattern));
    }

    /**
     * Gets a file with a specific path.
     * The file will be recorded as a dependency of all methods on the call stack.
     * @param name a file path within {@link #sourcePath()}
     * @return a File object referencing the specified path
     */
    default File sourceFile(String name) {
        return dependsOn(new File(Path.of(sourcePath(), name)));
    }

    /**
     * Executes an external process
     * @param command the command and arguments
     */
    default void exec(String... command) {
        Cmd.args(command).run();
    }

    /**
     * Creates a file and writes content to it
     * @param name a file path within {@link #buildPath()}
     * @param content the content to write into the file
     * @return a File object referencing the created file
     */
    default File write(String name, String content) {
        Path path = Path.of(buildPath(), name);
        try {
            Files.createDirectories(path.getParent());
            return new File(Files.writeString(path, content));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
