import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.copalis.jam.util.Args;
import org.copalis.jam.util.Compiler;
import org.copalis.jam.util.IvyResolver;
import org.copalis.jam.util.PackageResolver;
import org.copalis.jam.util.Paths;

/**
 * A Project with functionality for building Java code.
 * Utility methods are provided for compiling Java code,
 * running unit tests,
 * generating JavaDoc,
 * and creating jar files.
 * <p>
 * In order to enable dependency package downloading via the {@link #resolve(String...)} method,
 * an interface that implements {@link #packageResolver()} must be mixed in with this interface.
 *
 * @author gilesjb
 */
public interface JavaProject extends FileProject {

    /**
     * Runs a Java process.
     * @param args arguments to be supplied to the Java runtime
     * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/specs/man/java.html">Java command documentation</a>
     */
    default void java(String... args) {
        Args.of("java").and(args).run();
    }

    /**
     * Compiles Java code.
     * @param path the directory path for class files, relative to {@link #buildPath()}
     * @param sources the source files to compile
     * @param args the command-line options to be passed to the javac compiler
     * @return a reference to the compiled {@code .class} files
     * @see <a href="https://docs.oracle.com/javase/8/docs/technotes/tools/windows/javac.html">javac</a>
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
     * The default implementation uses Apache Ivy to download packages from the Maven repository.
     * @return the package resolver
     */
    default PackageResolver packageResolver() {
        return new IvyResolver(IvyResolver.VER2_5_1_URL, pkgCachePath());
    }

    /**
     * Gets dependencies
     * @param identifiers names of dependencies in the format {@code "org:name:revision"}.
     * If org and name are the same, the format {@code "name:revision"} can be used instead.
     * @return a Fileset containing references to the fetched dependencies
     */
    default Fileset resolve(String... identifiers) {
        return Fileset.of(packageResolver().resolve(identifiers));
    }

    /**
     * Gets the location of the package cache
     * @return the path of the package cache directory
     */
    default String pkgCachePath() {
        return ".package-cache";
    }

    /**
     * Deletes the package resolver's cache
     */
    default void cleanPkgCache() {
        Paths.rmDir(Path.of(pkgCachePath()));
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
     * @see <a href="https://docs.junit.org/current/user-guide/#running-tests-console-launcher">JUnit console documentation</a>
     */
    default Fileset junit(String reportsDir, String... args) {
        String output = buildPath(reportsDir);
        Args vmArgs = Args.of();
        Args junitArgs = Args.of();

        for (String arg : args) {
            if (arg.startsWith("-javaagent")) {
                vmArgs = vmArgs.and(arg);
            } else {
                junitArgs = junitArgs.and(arg);
            }
        }

        java(Args.of()
            .andAll(vmArgs)
            .and("-jar", jUnitLib().toString(), "--reports-dir=" + output)
            .andAll(junitArgs)
            .array());

        return builtFiles(reportsDir + "/**");
    }

    /**
     * Runs the JavaDoc tool
     * @param destination the path of generated unit test report, relative to {@link #buildPath()}
     * @param args the command-line arguments for the tool
     * @return a reference to the generated documentation
     * @see <a href="https://docs.oracle.com/javase/8/docs/technotes/tools/windows/javadoc.html">Javadoc</a>
     */
    default Fileset javadoc(String destination, String... args) {
        String dest = buildPath(destination);
        Compiler.javadoc(Args.of("-d", dest).and(args).array());
        return Fileset.find(dest +  "/**");
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
            try (JarOutputStream out = new JarOutputStream(stream)) {
                for (Fileset fs : contents) {
                    Path base = Path.of(fs.root);
                    for (File file : fs) {
                        JarEntry entry = new JarEntry(base.relativize(file.toPath()).toString());
                        entry.setTime(0);
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
