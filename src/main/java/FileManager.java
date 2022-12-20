import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;

public class FileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
    final Set<JavaFileObject> javaFiles = new HashSet<>();

    FileManager(StandardJavaFileManager fm) {
        super(fm);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(
            Location location, String className, Kind kind, FileObject sibling) throws IOException {
        JavaFileObject file = super.getJavaFileForOutput(location, className, kind, sibling);
        if (file.getKind() == JavaFileObject.Kind.CLASS) {
            javaFiles.add(file);
        }
        return file;
    }

    Set<JavaFileObject> javaFiles() {
        return javaFiles;
    }

    Fileset classFiles() {
        return new Fileset(javaFiles.stream()
                .map(JavaFileObject::toUri)
                .map(File::new)
                .collect(Collectors.toSet()));
    }
}