import java.util.Arrays;
import java.util.HashSet;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.copalis.builder.ClassFileManager;

public interface JavaProject extends Project {
    default JavaCompiler javaCompiler() {
        return ToolProvider.getSystemJavaCompiler();
    }

    default Fileset jarfiles(String dir) {
        return memo.dependsOn(Fileset.find(dir, "*.jar"));
    }

    default Fileset javac(Fileset sourceFiles, Fileset classpath, String directory) {
        return javac(sourceFiles, "-cp", classpath.base, "-d", directory);
    }

    default Fileset javac(Fileset sourceFiles, String... options) {
        JavaCompiler compiler = javaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        HashSet<JavaFileObject> outputClasses = new HashSet<>();
        compiler
            .getTask(
                    null, ClassFileManager.wrap(fileManager, outputClasses), d -> { }, Arrays.asList(options), null,
                    fileManager.getJavaFileObjectsFromFiles(sourceFiles))
            .call();

        return outputClasses.stream()
                .map(JavaFileObject::toUri)
                .map(File::new)
                .collect(Fileset.COLLECT);
    }
}
