package org.copalis.builder;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import javax.tools.DocumentationTool;
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
    private final DocumentationTool documenter = ToolProvider.getSystemDocumentationTool();

    StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

    public Stream<URI> compile(Iterable<? extends File> sourceFiles, String... options) {

        List<URI> outputClasses = new LinkedList<>();

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

        compiler
            .getTask(
                    null, outputs, null, Arrays.asList(options), null,
                    fileManager.getJavaFileObjectsFromFiles(sourceFiles))
            .call();

        return outputClasses.stream();
    }

    public void javadoc(Iterable<? extends File> sourceFiles, String... options) {
        documenter.run(null, null, null, options);
    }
}
