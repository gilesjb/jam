package org.copalis.jam;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.tools.DocumentationTool;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * Java build utilities
 *
 * @author gilesjb
 */
public class Compiler {
    private Compiler() { }

    private static final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    private static final DocumentationTool documenter = ToolProvider.getSystemDocumentationTool();

    private static final StandardJavaFileManager fileManager =
            compiler.getStandardFileManager(null, null, null);

    /**
     * Compiles Java source files
     * @param sourceFiles the Java files to compile
     * @param options options to be passed to java
     * @return a List of URIs of the output class files
     */
    public static List<URI> compile(Iterable<? extends File> sourceFiles, String... options) {

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

        if (!compiler
            .getTask(
                    null, outputs, null, Arrays.asList(options), null,
                    fileManager.getJavaFileObjectsFromFiles(sourceFiles))
            .call()) {
            throw new RuntimeException("Compilation failed");
        }

        return outputClasses;
    }

    /**
     * Generates JavaDoc
     * @param options options to be passed to javadoc
     */
    public static void javadoc(String... options) {
        if (documenter.run(null, null, null, options) != 0) {
            throw new RuntimeException("JavaDoc failed");
        }
    }
}
