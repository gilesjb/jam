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
            System.out.println("Downloaded " + path);
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
     * @param library a library identifier in the format org:name:version
     * @return a reference to the jar files
     */
    default Fileset requires(String library) {
        String[] lib = library.split(":");
        try {
            java.io.File pathFile = File.createTempFile("tmp-", ".path");
            String ivyCachePath = ivyCachePath();
            exec("java", "-jar", ivyJar(ivyCachePath),
                    "-warn",
                    "-dependency", lib[0], lib[1], lib[2],
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
     * @param library a library identifier in the format org:name:version:main-class
     * @param classpath additional jar files to use on the classpath
     * @param args the main class arguments
     */
    default void execDependency(String library, Collection<File> classpath, String... args) {
        String cp = classpath.stream().map(File::toString).collect(Collectors.joining(":"));
        String[] lib = library.split(":");

        exec(Stream.concat(Stream.of(
                        "java", "-jar", ivyJar(ivyCachePath()),
                        "-warn",
                        "-dependency", lib[0], lib[1], lib[2],
                        "-cache", ivyCachePath(),
                        "-cp", cp, "-main", lib[3], "-args"),
                Stream.of(args)).toArray(String[]::new));
    }
}
