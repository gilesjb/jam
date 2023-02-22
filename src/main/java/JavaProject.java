import java.net.URI;
import java.util.HashSet;

import org.copalis.builder.Compiler;

public interface JavaProject extends Project {

    final Compiler compiler = new Compiler();

    default Fileset javac(String javaFiles, Fileset classpath, String directory) {
        Fileset sourceFiles = memo.dependsOn(Fileset.find(srcDir() + '/' + javaFiles, "**.java"));
        HashSet<URI> outputClasses = new HashSet<>();

        compiler.compile(sourceFiles, outputClasses, "-cp", classpath.base, "-d", buildDir() + '/' + directory);

        return outputClasses.stream()
                .map(File::new)
                .collect(Fileset.FILES);
    }
}
