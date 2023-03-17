import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.copalis.builder.Compiler;

public interface JavaProject extends Project {

    final Compiler compiler = new Compiler();

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
        return compiler.compile(sourceFiles, options)
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

        compiler.javadoc(sources, args);
        return Fileset.find(destination.toString(), "**");
    }

    default Fileset junit(Fileset testClasses, Fileset... classpath) {
        List<String> args = new LinkedList<>();
        args.add("java");
        args.add("-cp");
        args.add(classpath(classpath) + ':' + classpath(testClasses));
        args.add("org.junit.runner.JUnitCore");
        Path base = Path.of(testClasses.base());
        testClasses.forEach(file -> {
            if (file.getName().endsWith(".class")) {
                String name = base.relativize(file.toPath()).toString();
                args.add(name.replace('/', '.').substring(0, name.length() - ".class".length()));
            }
        });
        if (exec(args.toArray(new String[0])) != 0) {
            throw new RuntimeException("Unit tests failed");
        }
        return testClasses;
    }

    default int exec(String... command) {
        ProcessBuilder proc = new ProcessBuilder();
        proc.command(command);
        proc.redirectError(Redirect.INHERIT);
        proc.redirectOutput(Redirect.INHERIT);
        try {
            return proc.start().waitFor();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
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
