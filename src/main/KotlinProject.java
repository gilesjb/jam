import org.copalis.jam.util.Args;

/**
 * Extends {@link JavaProject} with functionality for building Kotlin applications and libraries.
 * <p>
 * Utility methods are provided for
 * <ul>
 * <li>{@link #kotlinc(String, Fileset, String...) compiling} Kotlin code
 * <li>{@link #kotlincJar(String, Fileset, String...) compiling} Kotlin code directly to a jar
 * <li>{@link #kotlincJava(String, Fileset, Fileset, String...) compiling} mixed Kotlin/Java sources
 * <li>running {@link #kotlin(String...) Kotlin} programs
 * <li>generating {@link #dokka(String, String...) Dokka} documentation
 * </ul>
 *
 * @author gilesjb
 */
public interface KotlinProject extends JavaProject {

    /**
     * Runs a Kotlin program.
     * @param args arguments to be supplied to the Kotlin runtime
     * @see <a href="https://kotlinlang.org/docs/command-line.html">Kotlin command documentation</a>
     */
    default void kotlin(String... args) {
        Args.of("kotlin").and(args).run();
    }

    /**
     * Compiles Kotlin code.
     * @param path the directory path for class files, relative to {@link #buildPath()}
     * @param sources the source files to compile
     * @param args the command-line options to be passed to the kotlinc compiler
     * @return a reference to the compiled {@code .class} files
     * @see <a href="https://kotlinlang.org/docs/command-line.html">kotlinc</a>
     */
    default Fileset kotlinc(String path, Fileset sources, String... args) {
        String dest = buildPath(path);
        exec(Args.of("kotlinc")
                .and(sources.toString())
                .and("-d", dest)
                .and(args)
                .array());
        return Fileset.find(dest + "/**.class");
    }

    /**
     * Compiles Kotlin code directly to a self-contained {@code .jar} archive,
     * bundling the Kotlin runtime with {@code -include-runtime}.
     * @param jarPath the location and name of the generated archive, relative to {@link #buildPath()}
     * @param sources the source files to compile
     * @param args additional command-line options to be passed to the kotlinc compiler
     * @return a reference to the generated archive
     * @see <a href="https://kotlinlang.org/docs/command-line.html">kotlinc</a>
     */
    default File kotlincJar(String jarPath, Fileset sources, String... args) {
        String path = buildPath() + '/' + jarPath;
        exec(Args.of("kotlinc")
                .and(sources.toString())
                .and("-include-runtime")
                .and("-d", path)
                .and(args)
                .array());
        return new File(path);
    }

    /**
     * Compiles mixed Kotlin and Java sources.
     * <p>
     * Mixed compilation requires two passes: {@code kotlinc} compiles Kotlin sources first
     * with the Java sources provided as context, then {@code javac} compiles the Java sources
     * against the Kotlin output. Getting this order wrong produces broken output.
     * @param path the directory path for class files, relative to {@link #buildPath()}
     * @param kotlinSources the Kotlin source files to compile
     * @param javaSources the Java source files to compile
     * @param args the command-line options to be passed to both compilers
     * @return a reference to all compiled {@code .class} files
     * @see <a href="https://kotlinlang.org/docs/mixing-java-kotlin-intellij.html">Mixing Java and Kotlin</a>
     */
    default Fileset kotlincJava(String path, Fileset kotlinSources, Fileset javaSources, String... args) {
        String dest = buildPath(path);

        // Pass 1: kotlinc compiles Kotlin sources, with Java sources visible for cross-references
        exec(Args.of("kotlinc")
                .and(kotlinSources.toString())
                .and(javaSources.toString())
                .and("-d", dest)
                .and(args)
                .array());

        // Pass 2: javac compiles Java sources against the Kotlin class output
        return javac(path, javaSources,
                Args.of("-cp", dest).and(args).array());
     }

    /**
     * Generates Dokka documentation for Kotlin (and Java) sources.
     * @param destination the path of the generated documentation, relative to {@link #buildPath()}
     * @param args the command-line arguments for the Dokka CLI
     * @return a reference to the generated documentation
     * @see <a href="https://kotlinlang.org/docs/dokka-cli.html">Dokka CLI documentation</a>
     */
    default Fileset dokka(String destination, String... args) {
        String dest = buildPath(destination);
        java(Args.of("-jar", resolve("org.jetbrains.dokka:dokka-cli:1.9.20").toString(), "-outputDir", dest).and(args).array());
        return Fileset.find(dest + "/**");
    }
 }
