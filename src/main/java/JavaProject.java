import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.copalis.builder.ClassFileManager;

public interface JavaProject extends Project {
    default JavaCompiler javaCompiler() {
        return ToolProvider.getSystemJavaCompiler();
    }

    default Fileset javac(Fileset sourceFiles, String... options) {
        JavaCompiler compiler = javaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        HashSet<JavaFileObject> outputClasses = new HashSet<>();
        compiler
            .getTask(
                    null, ClassFileManager.wrap(fileManager, outputClasses), null, Arrays.asList(options), null,
                    fileManager.getJavaFileObjectsFromFiles(sourceFiles))
            .call();

        return Fileset.of(outputClasses.stream()
                .map(JavaFileObject::toUri)
                .map(File::new)
                .collect(Collectors.toSet()));
    }

    default Fileset javac(String sourceFiles, String... options) {
        return javac(sourceFiles(sourceFiles), options);
    }
}
