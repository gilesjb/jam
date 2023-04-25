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
     * Gets the URL for downloading the Ivy library
     * @return the URL string
     */
    default String ivyJarURL() {
        return "https://repo1.maven.org/maven2/org/apache/ivy/ivy/2.5.1/ivy-2.5.1.jar";
    }

    /**
     * Gets the local Ivy cache location
     * @return the path of the Ivy cache
     */
    default String ivyCachePath() {
        return Path.of(System.getProperty("user.home"), ".jam").toString();
    }

    default void cleanIvyCache() {
        Paths.rmDir(Path.of(ivyCachePath()));
    }

    /**
     * Gets a reference to the Ivy jarfile, downloading it if necessary.
     * Calling {@link File#getParent()} on this reference returns the same value as {@link #ivyCachePath()}.
     * @return a reference to the Ivy jar.
     */
    default File ivyJar() {
        Path path = Path.of(ivyCachePath(), "ivy.jar");
        if (!path.toFile().exists()) {
            String ivyJarURL = ivyJarURL();
            System.out.print("Downloading " + ivyJarURL + "... ");
            Paths.download(path, ivyJarURL);
            System.out.println("Done");
        }
        return new File(path);
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
            File ivyJar = ivyJar();
            String ivyCachePath = ivyJar.getParent();
            exec("java", "-jar", ivyJar.toString(),
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

            File ivyJar = ivyJar();
            Args args = Args.of(
                    "java", "-jar", ivyJar.toString(),
                    "-ivy", ivyFile,
                    "-cache", ivyJar.getParent(),
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
    default void execLibrary(String library, Collection<File> classpath, String... args) {
        String cp = classpath.stream().map(File::toString).collect(Collectors.joining(":"));
        String[] lib = library.split(":");
        File ivyJar = ivyJar();

        exec(Args.of(
                "java", "-jar", ivyJar.toString(),
                "-dependency", lib[0], lib[1], lib[2],
                "-cache", ivyJar.getParent(),
                "-cp", cp, "-main", lib[3], "-args").add(args).array());
    }
}
