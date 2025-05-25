import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
     * @param sources the source files to compile
     * @param args the command-line options to be passed to the javac compiler
     * @return a reference to the compiled {@code .class} files
     */
    default Fileset javac(Fileset sources, String... args) {
        Set<File> classFiles = Compiler.compile(sources, args).stream()
                .map(Paths::fromURI)
                .map(File::new)
                .collect(Collectors.toSet());

        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals("-d")) {
                return new Fileset(classFiles, args[i + 1], "**.class");
            }
        }

        return null;
    }

    /**
     * Generates a classpath string suitable for the java compiler
     * @param filesets a group of filesets
     * @return a colon-delimited list of paths
     */
    default String classpath(Fileset... filesets) {
        return classpath(Stream.of(filesets));
    }

    /**
     * Generates a classpath string suitable for the java compiler
     * @param filesets a group of filesets
     * @return a colon-delimited list of paths
     */
    default String classpath(Stream<Fileset> filesets) {
        return filesets
                .flatMap(Fileset::pathElements)
                .map(File::toString)
                .collect(Collectors.joining(":"));
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
     * @param args command line arguments to the jUnit console runtime
     * @return a fileset referring to the unit test report
     */
    default Fileset junit(String... args) {
        executeJar(jUnitLib().stream().findFirst().get(), args);

        for (String arg : args) {
            if (arg.startsWith("--reports-dir=")) {
                return Fileset.find(arg.substring(arg.indexOf('=') + 1), "**");
            }
        }

        return null;
    }

    /**
     * Runs the JavaDoc tool. See {@code javadoc --help} for documentation of options.
     * @param args the command-line arguments for the tool
     * @return a reference to the generated documentation
     */
    default Fileset javadoc(String... args) {
        Compiler.javadoc(args);

        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals("-d")) {
                return Fileset.find(args[i + 1], "**");
            }
        }

        return null;
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
