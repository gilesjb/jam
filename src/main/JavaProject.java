import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.copalis.builder.Compiler;

/**
 * A Project with functionality for building Java code
 *
 * @author giles
 */
public interface JavaProject extends Project {

    /**
     * Compiles Java code
     * @param outputPath the build path of the generated {@code .class} files
     * @param sources the source files to compile
     * @param classpath references to {@code .jar} or {@code .class} files
     * @return a reference to the compiled {@code .class} files
     */
    default Fileset javaCompile(String outputPath, Fileset sources, Fileset... classpath) {
        Path destination = Path.of(buildPath()).resolve(outputPath);
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
        Path destination = Path.of(buildPath()).resolve(outputPath);
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
    default Fileset junit(Fileset testClasses, Fileset... classpath) {
        Path base = Path.of(testClasses.base());
        String[] args = Stream.concat(
                Stream.of("java", "-cp", classpath(classpath) + ':' + classpath(testClasses),
                        "org.junit.runner.JUnitCore"),
                testClasses.stream()
                    .filter(file -> file.getName().endsWith("Test.class"))
                    .map(file -> base.relativize(file.toPath()).toString())
                    .map(name -> name.replace('/', '.')
                            .substring(0, name.length() - ".class".length()))).toArray(String[]::new);

        exec(args);
        return testClasses;
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
        return Arrays.asList(classpath).stream()
                .flatMap(fs -> Objects.nonNull(fs.base()) ? Stream.of(fs.base()) : fs.stream().map(File::toString))
                .collect(Collectors.joining(":"));
    }
}
