import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.copalis.builder.Args;
import org.copalis.builder.Paths;

/**
 * Dependency management using Apache Ivy
 *
 * @author giles
 */
public interface IvyProject extends Project {

    /**
     * The address of the Ivy jar files
     */
    final String IVY_URL = "https://repo1.maven.org/maven2/org/apache/ivy/ivy/2.5.1/ivy-2.5.1.jar";

    /**
     * Downloads the Ivy library if it hasn't been downloaded already
     * @param cachePath the path to the download cache
     * @return a reference to the Ivy library jar
     */
    public static String ivyJar(String cachePath) {
        Path path = Paths.join(cachePath, "ivy.jar");
        if (!path.toFile().exists()) {
            System.out.print("Downloading " + IVY_URL + "... ");
            Paths.download(path, IVY_URL);
            System.out.println("Done");
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
                    "-dependency", lib[0], lib[1], lib[2],
                    "-cache", ivyCachePath,
                    "-cachepath", pathFile.toString());
            return dependencies(pathFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Downloads dependencies specified in an Ivy XML file
     * @param ivyFile the Ivy XML file
     * @param configs names of configurations
     * @return a reference to the jar files
     */
    default Fileset requiresIvy(String ivyFile, String... configs) {
        try {
            java.io.File pathFile = File.createTempFile("tmp-", ".path");
            String ivyCachePath = ivyCachePath();

            Args args = Args.of(
                    "java", "-jar", ivyJar(ivyCachePath),
                    "-ivy", ivyFile,
                    "-cache", ivyCachePath,
                    "-cachepath", pathFile.toString());
            if (configs.length > 0) {
                args.add("-confs").add(configs);
            }

            exec(args.array());
            return dependencies(pathFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extracts a classpath from a file and schedules it for deletion
     * @param pathFile a reference to a file containing a colon-delimited classpath
     * @return a Fileset refencing the jar files in the classpath
     * @throws IOException if the file cannot be scheduled for deletion
     */
    static Fileset dependencies(java.io.File pathFile) throws IOException {
        pathFile.deleteOnExit();
        return Fileset.of(Stream.of(Files.readString(pathFile.toPath()).trim().split(":"))
                .map(File::new)
                .toArray(File[]::new));
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
        String ivyCachePath = ivyCachePath();
        String[] lib = library.split(":");

        exec(Args.of(
                "java", "-jar", ivyJar(ivyCachePath),
                "-dependency", lib[0], lib[1], lib[2],
                "-cache", ivyCachePath,
                "-cp", cp, "-main", lib[3], "-args").add(args).array());
    }
}
