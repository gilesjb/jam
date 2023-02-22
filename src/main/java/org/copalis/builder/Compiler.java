package org.copalis.builder;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Set;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class Compiler {

    private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

    public Boolean compile(Iterable<? extends File> sourceFiles, Set<URI> outputClasses, String... options) {

        JavaFileManager outputs = new ForwardingJavaFileManager<JavaFileManager>(fileManager) {

            @Override public JavaFileObject getJavaFileForOutput(
                    Location location, String className, Kind kind, FileObject sibling) throws IOException {

                JavaFileObject file = super.getJavaFileForOutput(location, className, kind, sibling);
                if (file.getKind() == JavaFileObject.Kind.CLASS) {
                    outputClasses.add(file.toUri());
                }
                return file;
            }
        };

        return compiler
            .getTask(
                    null, outputs, d -> { }, Arrays.asList(options), null,
                    fileManager.getJavaFileObjectsFromFiles(sourceFiles))
            .call();
    }
}
