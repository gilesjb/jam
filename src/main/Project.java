import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.copalis.builder.BuildController;
import org.copalis.builder.Memorizer;
import org.copalis.builder.Paths;

/**
 * A build project that provides common file manipulation methods
 *
 * @author giles
 */
public interface Project {

    /**
     * The memorizer that memoizes method calls on interfaces derived from this
     */
    final Memorizer memo = new Memorizer();

    /**
     * Print help information
     */
    default void help() {
        System.out.println("Jam build system");
        System.out.println("Syntax: <build-script> <target> <target>...");
        System.out.println("Specify target 'targets' to see all build targets and their result types");
    }

    /**
     * Prints the project's targets
     */
    default void targets() {
        System.out.println("Project targets");
        Stream.of(getClass().getInterfaces())
                .map(Class::getMethods)
                .flatMap(Stream::of)
                .filter(m -> m.getParameterCount() == 0)
                .forEach(m -> System.out.println("  " + m.getName() + " : " + m.getReturnType().getSimpleName()));
    }

    /**
     * The default source file root directory. Override if the desired directory is not {@code src}
     * @return the path of the source directory
     */
    default String sourcePath() {
        return "src";
    }

    /**
     * The default root build directory. Override if the desired build directory is not {@code build}
     * @return the path of the build directory
     */
    default String buildPath() {
        return "build";
    }

    /**
     * Deletes the build directory
     */
    default void clean() {
        deleteBuildDir("");
        memo.resetCache();
    }

    /**
     * Deletes the specified directory path beneath the build directory
     * @param path location of the directory to delete, relative to the build directory
     */
    default void deleteBuildDir(String path) {
        Path root = Paths.join(buildPath(), path);
        if (Files.exists(root)) {
            try {
                Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                    @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }
                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Gets files matching a pattern
     * @param pattern a glob pattern to search files within {@link #sourcePath()}
     * @return a fileset of the matching files
     */
    default Fileset sourceFiles(String pattern) {
        return memo.dependsOn(Fileset.find(sourcePath() + '/' + pattern));
    }

    /**
     * Gets a file with a specific path
     * @param name a file path within {@link #sourcePath()}
     * @return a File object referencing the specified path
     */
    default File sourceFile(String name) {
        return new File(Path.of(sourcePath()).resolve(name));
    }

    /**
     * Downloads a web resource
     * @param name the path and filename within the build directory of the downloaded file
     * @param url the URL of the resource
     * @return a File object referencing the downloaded file
     */
    default File download(String name, String url) {
        Path path = Paths.join(buildPath(), name);
        Paths.download(path, url);
        return new File(path);
    }

    /**
     * Executes an external process
     * @param command the command and arguments
     */
    default void exec(String... command) {
        ProcessBuilder pb = new ProcessBuilder();
        pb.command(command);
        pb.redirectError(Redirect.INHERIT);
        pb.redirectOutput(Redirect.INHERIT);
        try {
            Process proc = pb.start();
            int status = proc.waitFor();
			if (status != 0) {
            	throw new RuntimeException("Process exited with status code: " + status);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a file and writes content to it
     * @param name a file path within {@link #buildPath()}
     * @param content the content to write into the file
     * @return a File object referencing the created file
     */
    default File write(String name, String content) {
        Path path = Paths.join(buildPath(), name);
        try {
            Files.createDirectories(path.getParent());
            return new File(Files.writeString(path, content));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Executes a project
     * @param <T> the project type
     * @param t the project class reference
     * @param fn a consumer that calls the default build method
     * @param args command line arguments
     */
    public static <T extends Project> void make(Class<T> t, Consumer<T> fn, String[] args) {
        make(t, o -> {
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
    public static <T extends Project> void make(Class<T> t, Function<T, ?> fn, String[] args) {
        new BuildController<>(memo, t).execute(fn, Project::buildPath, args);
    }
}
