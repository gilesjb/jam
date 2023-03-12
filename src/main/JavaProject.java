import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.copalis.builder.Compiler;

public interface JavaProject extends Project {

    final Compiler compiler = new Compiler();

    default Fileset javaCompile(String outputPath, Fileset sources, Fileset... classpath) {
        Path destination = Path.of(buildPath()).resolve(outputPath);
        String cp = Arrays.asList(classpath).stream()
                .flatMap(fs -> Objects.nonNull(fs.base()) ? Stream.of(fs.base()) : fs.stream().map(File::toString))
                .collect(Collectors.joining(":"));

        javac(sources, "-cp", cp, "-d", destination.toString());
        return Fileset.find(destination.toString(), "**.class");
    }

    default Stream<File> javac(Fileset sourceFiles, String... options) {
        return compiler.compile(sourceFiles, options)
                .map(File::new);
    }
}
