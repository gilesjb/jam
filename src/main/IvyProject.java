import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.copalis.builder.Paths;

/**
 * Dependency management using Apache Ivy
 *
 * @author giles
 */
public interface IvyProject extends Project {

    /**
     * Downloads the Ivy library if it hasn't been downloaded already
     * @param cachePath the path to the download cache
     * @return a reference to the Ivy library jar
     */
    public static String ivyJar(String cachePath) {
        Path path = Paths.join(cachePath, "ivy.jar");
        if (!path.toFile().exists()) {
            Paths.download(path, "https://repo1.maven.org/maven2/org/apache/ivy/ivy/2.5.1/ivy-2.5.1.jar");
        }
        return path.toString();
    }

    /**
     * Gets the local Ivy cache location
     * @return the path of the Ivy cache
     */
    default String ivyCachePath() {
        return Paths.join(System.getProperty("user.home"), ".jam").toString();
    }

    /**
     * Downloads a library and its dependencies
     * @param org organization
     * @param name library name
     * @param version library revision
     * @return a reference to the jar files
     */
    default Fileset dependsOn(String org, String name, String version) {
        try {
            java.io.File pathFile = File.createTempFile("tmp-", ".path");
            String ivyCachePath = ivyCachePath();
            exec("java", "-jar", ivyJar(ivyCachePath), "-error", "-dependency", org, name, version,
                    "-cache", ivyCachePath,
                    "-cachepath", pathFile.toString());
            File[] jars = Stream.of(Files.readString(pathFile.toPath()).trim().split(":")).map(File::new).toArray(File[]::new);
            pathFile.deleteOnExit();
            return Fileset.of(jars);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Downloads a library and its dependencies,
     * and executes a specified class
     * @param mainClass the main class to executes
     * @param org organization
     * @param name library name
     * @param version library revision
     * @param classpath additional jar files to use on the classpath
     * @param args the main class arguments
     */
    default void execDependency(String mainClass, String org, String name, String version, Collection<File> classpath, String... args) {
        String cp = classpath.stream().map(File::toString).collect(Collectors.joining(":"));

        exec(Stream.concat(Stream.of(
                        "java", "-jar", ivyJar(ivyCachePath()), "-error", "-dependency", org, name, version,
                        "-cache", ivyCachePath(),
                        "-cp", cp, "-main", mainClass, "-args"),
                Stream.of(args)).toArray(String[]::new));
    }
}
