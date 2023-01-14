import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

import org.copalis.builder.BuildController;
import org.copalis.builder.Memorizer;

public interface Project {

    final Memorizer memo = new Memorizer();

    /**
     * The default source file root directory. Override if the desired directory is not <tt>src</tt>.
     * @return the path of the source directory
     */
    default String srcDir() {
        return "src";
    }

    /**
     * The default root build directory. Override if the desired build directory is not <tt>build</tt>.
     * @return the path of the build directory
     */
    default String buildDir() {
        return "build";
    }

    /**
     * Gets files matching a pattern
     * @param pattern a glob pattern to search files within {@link #srcDir()}
     * @return a fileset of the matching files
     */
    default Fileset sourceFiles(String pattern) {
        return memo.dependsOn(Fileset.find(srcDir(), pattern));
    }

    /**
     * Gets a file with a specific path
     * @param name a file path within {@link #srcDir()}
     * @return a File object referencing the specified path
     */
    default File srcFile(String name) {
        return new File(Path.of(srcDir()).resolve(name));
    }

    /**
     * Creates a file and writes content to it
     * @param name a file path within {@link #buildDir()}
     * @param content
     * @return a File object referencing the created file
     */
    default File write(String name, String content) {
        Path path = Path.of(buildDir()).resolve(name);
        try {
            Files.createDirectories(path.getParent());
            return new File(Files.writeString(path, content));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T extends Project> void make(Class<T> t, Function<T, ?> fn, String[] args) {
        new BuildController<>(memo, t).execute(fn, Project::buildDir, args);
    }
}
