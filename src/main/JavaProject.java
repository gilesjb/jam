import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.copalis.jam.Cmd;
import org.copalis.jam.Compiler;
import org.copalis.jam.PackageResolver;
import org.copalis.jam.Paths;

/**
 * A Project with functionality for building Java code
 *
 * @author gilesjb
 */
public interface JavaProject extends BuilderProject {

    /**
     * Runs a Java process. The output of this process will not be tracked as a dependency.
     * @param args arguments to be supplied to the Java runtime
     */
    default void java(String... args) {
        Cmd.args("java").add(args).run();
    }

    /**
     * Compiles Java code.
     * @param path the directory path for class files, relative to {@link #buildPath()}
     * @param sources the source files to compile
     * @param args the command-line options to be passed to the javac compiler
     * @return a reference to the compiled {@code .class} files
     */
    default Fileset javac(String path, Fileset sources, String... args) {
        String dest = buildPath(path);

        Set<File> classFiles = Compiler.compile(sources,
                        Stream.concat(Stream.of("-d", dest), Arrays.stream(args)).toList())
                .stream()
                .map(Paths::fromURI)
                .map(File::new)
                .collect(Collectors.toSet());

        return new Fileset(classFiles, dest, "**.class");
    }

    /**
     * Generates a classpath string suitable for the java compiler
     * @param filesets a group of filesets
     * @return a colon-delimited list of paths
     */
    default String classpath(Fileset... filesets) {
        return Stream.of(filesets)
                .flatMap(Fileset::pathElements)
                .map(File::toString)
                .collect(Collectors.joining(System.getProperty("path.separator")));
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
        return packageResolver()
                .resolve(identifiers)
                .map(Paths::relativize)
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
     * Specifies the jUnit console and runtime libraries.
     * @return a dependency referencing the jUnit libraries
     */
    default Fileset jUnitLib() {
        return resolve("org.junit.platform:junit-platform-console-standalone:1.9.3");
    }

    /**
     * Runs unit tests using the jUnit console library specified by {@link #jUnitLib()}
     * @param reportsDir the path of generated unit test report, relative to {@link #buildPath()}
     * @param args command line arguments to the jUnit console runtime
     * @return a fileset referring to the unit test report
     */
    default Fileset junit(String reportsDir, String... args) {
        String output = buildPath(reportsDir);

        executeJar(jUnitLib().stream().findFirst().get(),
                Stream.concat(Stream.of("--reports-dir=" + output),
                        Arrays.stream(args)).toArray(String[]::new));

        return Fileset.find(output, "**");
    }

    /**
     * Runs the JavaDoc tool. See {@code javadoc --help} for documentation of options.
     * @param destination the path of generated unit test report, relative to {@link #buildPath()}
     * @param args the command-line arguments for the tool
     * @return a reference to the generated documentation
     */
    default Fileset javadoc(String destination, String... args) {
        String dest = buildPath(destination);
        Compiler.javadoc(Stream.concat(Stream.of("-d", dest), Arrays.stream(args)).toArray(String[]::new));
        return Fileset.find(dest, "**");
    }

    /**
     * Executes a jar file
     * @param jar the jar file
     * @param args command line arguments
     */
    default void executeJar(File jar, String... args) {
        Cmd.args("java", "-jar", jar.toString()).add(args).run();
    }

    /**
     * Creates a {@code .jar} archive
     * @param jarPath the location and name of the generated archive
     * @param contents references to the files that should be placed in the archive
     * @return a reference to the generated archive
     */
    default File jar(String jarPath, Fileset... contents) {
        String path = buildPath() + '/' + jarPath;
        try (FileOutputStream stream = new FileOutputStream(path)) {
            try (JarOutputStream out = new JarOutputStream(stream, new Manifest())) {
                for (Fileset fs : contents) {
                    Path base = Path.of(fs.base);
                    for (File file : fs) {
                        JarEntry entry = new JarEntry(base.relativize(file.toPath()).toString());
                        entry.setTime(file.lastModified());
                        out.putNextEntry(entry);
                        Files.copy(file.toPath(), out);
                        out.closeEntry();
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new File(path);
    }
}
