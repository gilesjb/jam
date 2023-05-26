import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.copalis.jam.Cmd;
import org.copalis.jam.Paths;

/**
 * Logic for using Ivy dependency resolution
 *
 * @author gilesjb
 */
public interface Ivy extends Serializable {

    /**
     * Represents the Ivy executable
     * @param cacheDir the path of the Ivy cache
     * @param url the URL that Ivy can be downloaded from
     */
    record Runtime(String cacheDir, String url) implements Ivy {

        private Path ivyJar() {
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
                java.io.File pathFile = File.createTempFile("classpath-", null);
                Cmd.of("java", "-jar", ivyJar().toString(), "-cache", cacheDir,
                        "-cachepath", pathFile.toString())
                    .add(depends.dependencyArgs())
                    .exec();

                pathFile.deleteOnExit();
                return Fileset.of(Stream.of(Files.readString(pathFile.toPath()).trim().split(":"))
                        .map(File::new)
                        .toArray(File[]::new));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Generates the command line arguments to execute the main class of the specified dependency
         * @param runnable a dependency with a main class
         * @param classpath additional classpath elements
         * @param args additional command line arguments
         * @return the complete Ivy command line arguments
         */
        Cmd command(Executable runnable, Collection<File> classpath, String... args) {
            Cmd cmd = Cmd.of(
                    "java", "-jar", ivyJar().toString(), "-cache", cacheDir)
                .add(runnable.runArgs());

            if (!classpath.isEmpty()) {
                cmd.add("-cp", classpath.stream()
                        .map(File::toString).collect(Collectors.joining(":")));
            }

            if (args.length > 0) {
                cmd.add("-args").add(args);
            }

            return cmd;
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
        Cmd dependencyArgs();

        /**
         * Adds a reference to an Ivy settings file
         * @param settingsFile a reference to an XML settings file
         * @return a dependency object including the reference
         */
        default Dependency addSettings(File settingsFile) {
            return addOptions("-settings", settingsFile.toString());
        }

        /**
         * Adds a reference to an Ivy settings file
         * @param settingsFile a reference to an XML settings file
         * @param propertiesFile a reference to an additional properties file
         * @return a dependency object including the references
         */
        default Dependency addSettings(File settingsFile, File propertiesFile) {
            return addSettings(settingsFile).addOptions("-properties", propertiesFile.toString());
        }

        /**
         * Adds dependency resolution command line arguments
         * @param args arguments
         * @return a dependency object including the arguments
         */
        default Dependency addOptions(String... args) {
            return new Options(dependencyArgs().add(args).array());
        }

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
        Cmd args = Cmd.of("-ivy", ivyFile.toString());
        if (confs.length > 0) {
            args.add("-confs").add(confs);
        }
        return new Options(args.array());
    }

    /**
     * Creates a dependency that is specified by org, name and version
     * @param org the organization
     * @param name the artifact name
     * @param version the artifact version number
     * @param confs the names of configurations to use
     * @return the dependency object
     */
    static Dependency namedDependency(String org, String name, String version, String... confs) {
        Cmd args = Cmd.of("-dependency", org, name, version);
        if (confs.length > 0) {
            args.add("-confs").add(confs);
        }
        return new Options(args.array());
    }

    /**
     * The Ivy options required to fetch a dependency
     * @param args the arguments
     */
    record Options(String... args) implements Dependency {
        public Cmd dependencyArgs() {
            return Cmd.of(args);
        }
        @Override public String toString() {
            return "IvyArgs" + Arrays.asList(args);
        }
    }

    /**
     * A dependency with an associated executable main class
     * @param dependency the dependency
     * @param main the name of the main class
     */
    record Executable(Dependency dependency, String main) implements Dependency {
        public Cmd dependencyArgs() {
            return dependency.dependencyArgs();
        }

        /**
         * Gets the Ivy arguments required to execute the main class of this dependency
         * @return an Args instance
         */
        public Cmd runArgs() {
            return dependencyArgs().add("-main", main);
        }
    }
}
