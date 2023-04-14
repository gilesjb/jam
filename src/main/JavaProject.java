import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.copalis.builder.Compiler;
import org.copalis.builder.Paths;

/**
 * A Project with functionality for building Java code
 *
 * @author giles
 */
public interface JavaProject extends IvyProject {

    /**
     * Compiles Java code
     * @param outputPath the build path of the generated {@code .class} files
     * @param sources the source files to compile
     * @param classpath references to {@code .jar} or {@code .class} files
     * @return a reference to the compiled {@code .class} files
     */
    default Fileset javaCompile(String outputPath, Fileset sources, Fileset... classpath) {
        Path destination = Paths.join(buildPath(), outputPath);
        try {
            Files.createDirectories(destination);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String cp = classpath(classpath);

        if (cp.isEmpty()) {
            javac(sources, "-d", destination.toString());
        } else {
            javac(sources, "-d", destination.toString(), "-cp", cp);
        }
        return Fileset.find(destination.toString(), "**.class");
    }

    /**
     * Compiles Java unit tests
     * @param outputPath the build path of the generated {@code .class} files
     * @param sources the source files to compile
     * @param classpath references to {@code .jar} or {@code .class} files
     * @return a reference to the compiled {@code .class} files
     */
    default Fileset javaTestCompile(String outputPath, Fileset sources, Fileset... classpath) {
        return javaCompile(outputPath, sources, Stream.concat(Stream.of(classpath),
                Stream.of(dependsOn("junit", "junit", "4.13.2"))).toArray(Fileset[]::new));
    }

    /**
     * Compiles Java code
     * @param sourceFiles the source files to compile
     * @param options to pass to the javac compiler
     * @return a stream of the generated {@code .class} files
     */
    default Stream<File> javac(Fileset sourceFiles, String... options) {
        return Compiler.compile(sourceFiles, options)
                .map(File::new);
    }

    /**
     * Generates JavaDoc
     * @param outputPath the path of generated documents
     * @param sources the source files to document
     * @param packages names of packages to document
     * @return a reference to the generated documentation
     */
    default Fileset javadoc(String outputPath, Fileset sources, String... packages) {
        Path destination = Paths.join(buildPath(), outputPath);
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
     * Runs unit tests
     * @param testClasses a reference to the test {@code .class} files
     * @param classpath references to {@code .jar} or {@code .class} files
     * @return the test class files
     */
    default Fileset jUnit(Fileset testClasses, Fileset... classpath) {
        Path base = Path.of(testClasses.base());

        String[] args = testClasses.stream()
            .filter(file -> file.getName().endsWith("Test.class"))
            .map(file -> base.relativize(file.toPath()).toString())
            .map(name -> name.replace('/', '.').substring(0, name.length() - ".class".length()))
            .toArray(String[]::new);

        List<File> files = Stream.concat(Stream.of(testClasses), Stream.of(classpath))
                .flatMap(fs -> Objects.nonNull(fs.base()) ? Stream.of(new File(fs.base())) : fs.stream())
                .collect(Collectors.toList());

        execDependency("org.junit.runner.JUnitCore", "junit", "junit", "4.13.2", files, args);
        return testClasses;
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
        exec(Stream.concat(Stream.of("java", "-cp", cp, className), Stream.of(args)).toArray(String[]::new));
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
