import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.copalis.jam.BuildController;
import org.copalis.jam.Cmd;
import org.copalis.jam.PackageResolver;
import org.copalis.jam.Paths;

/**
 * A project that reads and writes to the file system
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
     * Deletes the build directory and empties the method cache
     */
    default void clean() {
        rmBuildDir("");
        Project.super.clean();
    }

    /**
     * Gets the package dependency resolver.
     * @return the package resolver
     */
    PackageResolver packageResolver();

    /**
     * Gets dependencies
     * @param identifiers names of dependencies in the format {@code "org:name:revision"}
     * @return a Fileset containing references to the fetched dependencies
     */
    default Fileset resolve(String... identifiers) {
        return packageResolver().resolve(identifiers)
                .map(File::new)
                .collect(Fileset.FILES);
    }

    /**
     * Deletes the package resolver's cache
     */
    default void cleanPkgCache() {
        packageResolver().cleanCache();
    }

    /**
     * Recursively deletes a specified build directory and its contents
     * @param path a directory path relative to {@link #buildPath()}
     */
    default void rmBuildDir(String path) {
        Paths.rmDir(Path.of(buildPath(), path));
    }

    /**
     * Gets source files matching a pattern.
     * The matching files will be recorded as dependencies of all methods on the call stack.
     * @param pattern a glob pattern to search files within {@link #sourcePath()}
     * @return a fileset of the matching files
     */
    default Fileset sourceFiles(String pattern) {
        return get(BuildController.MEMO).dependsOn(Fileset.find(sourcePath() + '/' + pattern));
    }

    /**
     * Gets a file with a specific path.
     * The file will be recorded as a dependency of all methods on the call stack.
     * @param name a file path within {@link #sourcePath()}
     * @return a File object referencing the specified path
     */
    default File sourceFile(String name) {
        return get(BuildController.MEMO).dependsOn(new File(Path.of(sourcePath(), name)));
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
