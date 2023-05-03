import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.copalis.builder.Args;
import org.copalis.builder.Paths;

/**
 * Logic for using Ivy dependency resolution
 *
 * @author gilesjb@gmail.com
 */
public interface Ivy extends Serializable {

    /**
     * Represents the Ivy executable
     * @param cacheDir the path of the Ivy cache
     * @param url the URL that Ivy can be downloaded from
     */
    record Runtime(String cacheDir, String url) implements Ivy {

        private Path jar() {
            Path path = Path.of(cacheDir, url.substring(url.lastIndexOf('/') + 1));
            if (!path.toFile().exists()) {
                System.out.print("Downloading " + url + "... ");
                Paths.download(path, url);
                System.out.println("Done");
            }
            return path;
        }

        /**
         * Fetches the specified dependency and all its transitive dependencies
         * @param depends a dependency
         * @return a fileset referencing the jars
         */
        Fileset requires(Dependency depends) {
            try {
                java.io.File pathFile = File.createTempFile("tmp-", ".path");
                Args.of("java", "-jar", jar().toString(),
                        "-cache", cacheDir,
                        "-cachepath", pathFile.toString()).add(depends.dependencyArgs()).exec();

                pathFile.deleteOnExit();
                return Fileset.of(Stream.of(Files.readString(pathFile.toPath()).trim().split(":"))
                        .map(File::new)
                        .toArray(File[]::new));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Runs the main class of the specified dependency
         * @param runnable a dependency with a main class
         * @param classpath additional classpath elements
         * @param args command line arguments
         */
        void execute(Executable runnable, Collection<File> classpath, String... args) {
            String cp = classpath.stream().map(File::toString).collect(Collectors.joining(":"));

            Args.of("java", "-jar", jar().toString())
                .add(runnable.runArgs())
                .add(
                    "-cache", cacheDir,
                    "-cp", cp,
                    "-args").add(args).exec();
        }
    }

    /**
     * A dependency
     */
    public interface Dependency extends Ivy {
        /**
         * Gets the Ivy arguments necessary to resolve this dependency
         * @return an Args instance
         */
        Args dependencyArgs();

        /**
         * Associates a main class with this dependency
         * @param mainClass the name of the class with a {@code main()} method
         * @return an {@link Executable}
         */
        default Executable mainClass(String mainClass) {
            return new Executable(this, mainClass);
        }
    }

    /**
     * Create a dependency that is specified by an Ivy XML file
     * @param ivyFile the source location of the Ivy XML file
     * @param confs the names of configurations to use
     * @return the dependency object
     */
    static Dependency configuredDependency(File ivyFile, String... confs) {
        Args args = Args.of("-ivy", ivyFile.toString());
        if (confs.length > 0) {
            args.add("-confs").add(confs);
        }
        return new DependencyArgs(args.array());

    }

    /**
     * Creates a dependency that is specified by org, name and version
     * @param org the organization
     * @param name the artifact name
     * @param version the artifact version number
     * @return the dependency object
     */
    static Dependency namedDependency(String org, String name, String version) {
        return new DependencyArgs("-dependency", org, name, version);
    }

    /**
     * The Ivy arguments required to fetch a dependency
     * @param args the arguments
     */
    record DependencyArgs(String... args) implements Dependency {
        public Args dependencyArgs() {
            return Args.of(args);
        }
        @Override public String toString() {
            return Arrays.asList(args).toString();
        }
    }

    /**
     * A dependency with an associated executable main class
     * @param dependency the dependency
     * @param mainClass the name of the main class
     */
    record Executable(Dependency dependency, String mainClass) implements Dependency {
        public Args dependencyArgs() {
            return dependency.dependencyArgs();
        }

        /**
         * Gets the Ivy arguments required to execute the main class of this dependency
         * @return an Args instance
         */
        public Args runArgs() {
            return dependencyArgs().add("-main", mainClass);
        }
    }
}