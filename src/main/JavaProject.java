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

public interface JavaProject extends Project {

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
            javac(sources, "-cp", cp, "-d", destination.toString());
        }
        return Fileset.find(destination.toString(), "**.class");
    }

    default Stream<File> javac(Fileset sourceFiles, String... options) {
        return Compiler.compile(sourceFiles, options)
                .map(File::new);
    }

    default Fileset javadoc(String outputPath, Fileset sources, String... packages) {
        Path destination = Path.of(buildPath()).resolve(outputPath);
        try {
            Files.createDirectories(destination);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String[] args = Stream.concat(
                Stream.of("-d", destination.toString(), "-sourcepath", sources.base()),
                Stream.of(packages)).toArray(String[]::new);

        Compiler.javadoc(args);
        return Fileset.find(destination.toString(), "**");
    }

    default Fileset junit(Fileset testClasses, Fileset... classpath) {
        Path base = Path.of(testClasses.base());
        String[] args = Stream.concat(
                Stream.of("java", "-cp", classpath(classpath) + ':' + classpath(testClasses),
                        "org.junit.runner.JUnitCore"),
                testClasses.stream()
                    .filter(file -> file.getName().endsWith(".class"))
                    .map(file -> base.relativize(file.toPath()).toString())
                    .map(name -> name.replace('/', '.')
                            .substring(0, name.length() - ".class".length()))).toArray(String[]::new);

        if (exec(args) != 0) {
            throw new RuntimeException("Unit tests failed");
        }
        return testClasses;
    }

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

    static String classpath(Fileset... classpath) {
        return Arrays.asList(classpath).stream()
                .flatMap(fs -> Objects.nonNull(fs.base()) ? Stream.of(fs.base()) : fs.stream().map(File::toString))
                .collect(Collectors.joining(":"));
    }
}
