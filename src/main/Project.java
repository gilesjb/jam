

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Consumer;
import java.util.function.Function;

import org.copalis.builder.BuildController;
import org.copalis.builder.Memorizer;

public interface Project {

    final Memorizer memo = new Memorizer();

    /**
     * The default source file root directory. Override if the desired directory is not <tt>src</tt>.
     * @return the path of the source directory
     */
    default String sourcePath() {
        return "src";
    }

    /**
     * The default root build directory. Override if the desired build directory is not <tt>build</tt>.
     * @return the path of the build directory
     */
    default String buildPath() {
        return "build";
    }

    default void clean() {
        deleteDir(buildPath());
    }

    default void deleteDir(String path) {
        Path root = Path.of(path);
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

    default File download(String name, String url) {
        try {
            Path path = Path.of(buildPath()).resolve(name);
            Files.createDirectories(path.getParent());
            try (ReadableByteChannel readableByteChannel = Channels.newChannel(new URL(url).openStream());
                    FileOutputStream out = new FileOutputStream(path.toFile())) {
                out.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                return new File(path);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a file and writes content to it
     * @param name a file path within {@link #buildPath()}
     * @param content
     * @return a File object referencing the created file
     */
    default File write(String name, String content) {
        Path path = Path.of(buildPath()).resolve(name);
        try {
            Files.createDirectories(path.getParent());
            return new File(Files.writeString(path, content));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T extends Project> void make(Class<T> t, Consumer<T> fn, String[] args) {
        make(t, o -> {
            fn.accept(o);
            return null;
        }, args);
    }

    public static <T extends Project> void make(Class<T> t, Function<T, ?> fn, String[] args) {
        new BuildController<>(memo, t).execute(fn, Project::buildPath, args);
    }
}
