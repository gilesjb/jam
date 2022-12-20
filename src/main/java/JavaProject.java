import java.util.Arrays;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public interface JavaProject extends Project {

    default JavaCompiler javaCompiler() {
        return ToolProvider.getSystemJavaCompiler();
    }

    default Fileset javac(Fileset sourceFiles, String... options) {
        JavaCompiler compiler = javaCompiler();
        StandardJavaFileManager javaFileManager = compiler.getStandardFileManager(null, null, null);

        FileManager fileManager = new FileManager(javaFileManager);

        compiler
            .getTask(
                    null, fileManager, null, Arrays.asList(options), null,
                    javaFileManager.getJavaFileObjectsFromFiles(sourceFiles))
            .call();

        return fileManager.classFiles();
    }

    default Fileset javac(String sourceFiles, String... options) {
        return javac(sourceFiles(sourceFiles), options);
    }
}
