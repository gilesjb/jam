import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.copalis.jam.Args;
import org.copalis.jam.Compiler;
import org.copalis.jam.Paths;

/**
 * A Project with functionality for building Java code
 *
 * @author gilesjb@gmail.com
 */
public interface JavaProject extends IvyProject {

    /**
     * Compiles Java code
     * @param outputPath the location within the build path to place the generated {@code .class} files
     * @param sources the source files to compile
     * @param classpath references to {@code .jar} or {@code .class} files
     * @return a reference to the compiled {@code .class} files
     * @see #buildPath()
     */
    default Fileset javaCompile(String outputPath, Fileset sources, Fileset... classpath) {
        Path destination = Path.of(buildPath(), outputPath);
        try {
            Files.createDirectories(destination);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String cp = classpath(classpath);
        final List<URI> uris;

        if (cp.isEmpty()) {
            uris = Compiler.compile(sources, "-d", destination.toString());
        } else {
            uris = Compiler.compile(sources, "-d", destination.toString(), "-cp", cp);
        }
        Set<File> classFiles = uris.stream()
                .map(Paths::fromURI).map(File::new)
                .collect(Collectors.toSet());
        return new Fileset(classFiles, destination.toString(), "**.class");
    }

    /**
     * Gets the unit test library identifier.
     * By default this is JUnit 4.13.2.
     * @return a library identifier
     */
    default Ivy.Executable unitTestLibrary() {
        return Ivy.namedDependency("junit", "junit", "4.13.2")
                .mainClass("org.junit.runner.JUnitCore");
    }

    /**
     * Compiles Java unit tests
     * @param outputPath the location within the build path to save the generated {@code .class} files
     * @param sources the source files to compile
     * @param classpath references to {@code .jar} or {@code .class} files
     * @return a reference to the compiled {@code .class} files
     * @see #buildPath()
     */
    default Fileset javaTestCompile(String outputPath, Fileset sources, Fileset... classpath) {
        return javaCompile(outputPath, sources, Stream.concat(Stream.of(classpath),
                Stream.of(requires(unitTestLibrary()))).toArray(Fileset[]::new));
    }

    /**
     * Runs unit tests using the library specified by {@link #unitTestLibrary()}
     * @param testClasses a reference to the test {@code .class} files
     * @param classpath references to {@code .jar} or {@code .class} files
     * @return the test class files
     */
    default Fileset jUnit(Fileset testClasses, Fileset... classpath) {
        Path base = Path.of(testClasses.base());

        String[] tests = testClasses.stream()
            .filter(file -> file.getName().endsWith("Test.class"))
            .map(file -> base.relativize(file.toPath()).toString())
            .map(name -> name.replace('/', '.').substring(0, name.length() - ".class".length()))
            .toArray(String[]::new);

        List<File> files = Stream.concat(Stream.of(testClasses), Stream.of(classpath))
                .flatMap(fs -> Objects.nonNull(fs.base()) ? Stream.of(new File(fs.base())) : fs.stream())
                .collect(Collectors.toList());

        exec(unitTestLibrary(), files, tests);
        return testClasses;
    }

    /**
     * Generates JavaDoc
     * @param outputPath the location within the build path to save the generated documents
     * @param sources the source files to document
     * @param packages names of packages to document
     * @return a reference to the generated documentation
     * @see #buildPath()
     */
    default Fileset javadoc(String outputPath, Fileset sources, String... packages) {
        Path destination = Path.of(buildPath(), outputPath);
        try {
            Files.createDirectories(destination);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String[] args = Stream.concat(
                Stream.of("-d", destination.toString(), "-sourcepath", sources.base(), "-quiet"),
                packages.length > 0 ? Stream.of(packages) : sources.stream().map(File::toString)
            ).toArray(String[]::new);

        Compiler.javadoc(args);
        return Fileset.find(destination.toString(), "**");
    }

    /**
     * Executes the {@code main()} method of a specified class,
     * running it in a new Java process
     * @param className the class name
     * @param classpath the classpath to use
     * @param args arguments to be passed to the main method
     * @see #runMain(String, Collection, String...)
     */
    default void execMain(String className, Collection<File> classpath, String... args) {
        String cp = classpath.stream().map(File::toString).collect(Collectors.joining(":"));
        Args.of("java", "-cp", cp, className).add(args).exec();
    }

    /**
     * Executes the {@code main()} method of a specified class,
     * running it in the current process
     * @param className the class name
     * @param classpath the classpath to use
     * @param args arguments to be passed to the main method
     * @see #execMain(String, Collection, String...)
     */
    default void runMain(String className, Collection<File> classpath, String... args) {
        URL[] urls = classpath.stream().map(File::getURL).toArray(URL[]::new);

        Thread thread = Thread.currentThread();
        try (URLClassLoader loader = new URLClassLoader(urls, this.getClass().getClassLoader())) {
            thread.setContextClassLoader(loader);
            loader.loadClass(className)
                    .getDeclaredMethod("main", String[].class)
                    .invoke(null, (Object) args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

    /**
     * Generates a classpath string
     * @param classpath collections of {@code .class} or {@code .jar} files
     * @return a colon-separated classpath string
     */
    static String classpath(Fileset... classpath) {
        return Stream.of(classpath)
                .flatMap(fs -> Objects.nonNull(fs.base()) ? Stream.of(fs.base()) : fs.stream().map(File::toString))
                .collect(Collectors.joining(":"));
    }
}
