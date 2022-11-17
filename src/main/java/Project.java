import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

import org.copalis.builder.Controller;
import org.copalis.builder.Memorizer;

public interface Project {

    final Memorizer memo = new Memorizer();

    /**
     * Returns the root source directory
     * @return the path of the source directory
     */
    default String srcDir() {
        return "src";
    }

    /**
     * Returns the root build directory
     * @return the path of the build directory
     */
    default String buildDir() {
        return "build";
    }

    /**
     * Gets files matching a pattern
     * @param pattern
     * @return a fileset of the matching files
     */
    default Fileset sourceFiles(String pattern) {
        return memo.dependsOn(Fileset.find(srcDir(), pattern));
    }

    /**
     * Gets the file with a specific path
     * @param name
     * @return a File object referencing the specified path
     */
    default File srcFile(String name) {
        return new File(Path.of(srcDir()).resolve(name));
    }

    /**
     * Creates a file and writes content to it
     * @param name
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

    static <T extends Project> void make(Class<T> t, Function<T, ?> fn, String[] args) {
        new Controller<>(memo, t).execute(fn, Project::buildDir, args);
    }
}
