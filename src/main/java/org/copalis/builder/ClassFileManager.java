package org.copalis.builder;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;

public class ClassFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
    final Set<JavaFileObject> classFiles = new HashSet<>();

    public ClassFileManager(StandardJavaFileManager fm) {
        super(fm);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(
            Location location, String className, Kind kind, FileObject sibling) throws IOException {
        JavaFileObject file = super.getJavaFileForOutput(location, className, kind, sibling);
        if (file.getKind() == JavaFileObject.Kind.CLASS) {
            classFiles.add(file);
        }
        return file;
    }

    public Stream<JavaFileObject> classFiles() {
        return classFiles.stream();
    }
}