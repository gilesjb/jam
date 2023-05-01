import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.copalis.builder.Args;

/**
 * Logic for using Ivy dependency resolution
 *
 * @author giles
 */
public interface Ivy extends Serializable {

    /**
     * Represents the Ivy executable
     * @param ivyJar the location of Ivy jar file
     */
    record Jar(File ivyJar) implements Ivy {
        /**
         * Fetches the specified dependency and all its transitive dependencies
         * @param depends a dependency
         * @return a fileset referencing the jars
         */
        Fileset requires(Dependency depends) {
            try {
                java.io.File pathFile = File.createTempFile("tmp-", ".path");
                Args.of("java", "-jar", ivyJar.toString(),
                        "-cache", ivyJar.getParent(),
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
            File ivyJar = ivyJar();

            Args.of("java", "-jar", ivyJar.toString())
                .add(runnable.runArgs())
                .add(
                    "-cache", ivyJar.getParent(),
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
     * A dependency that is specified by an Ivy XML files
     * @param ivyFile the Ivy XML file
     * @param confs the names of configurations to use
     */
    record ConfiguredDependency(String ivyFile, String... confs) implements Dependency {
        public Args dependencyArgs() {
            Args args = Args.of("-ivy", ivyFile);
            if (confs.length > 0) {
                args.add("-confs").add(confs);
            }
            return args;
        }
    }

    /**
     * A dependency that is specified by org, name and version
     * @param org the organization
     * @param name the artifact name
     * @param version the artifact version number
     */
    record NamedDependency(String org, String name, String version) implements Dependency {
        public Args dependencyArgs() {
            return Args.of("-dependency", org, name, version);
        }
        @Override public String toString() {
            return org + ':' + name + ':' + version;
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
