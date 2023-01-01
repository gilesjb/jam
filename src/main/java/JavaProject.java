import java.util.Arrays;
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
        StandardJavaFileManager javaFileManager = compiler.getStandardFileManager(null, null, null);

        ClassFileManager fileManager = new ClassFileManager(javaFileManager);

        compiler
            .getTask(
                    null, fileManager, null, Arrays.asList(options), null,
                    javaFileManager.getJavaFileObjectsFromFiles(sourceFiles))
            .call();

        return Fileset.of(fileManager.classFiles()
                .map(JavaFileObject::toUri)
                .map(File::new)
                .collect(Collectors.toSet()));
    }

    default Fileset javac(String sourceFiles, String... options) {
        return javac(sourceFiles(sourceFiles), options);
    }
}
