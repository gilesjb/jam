import java.nio.file.Path;
import java.util.Collection;

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
    default String jarCachePath() {
        return Path.of(System.getProperty("user.home"), ".jam").toString();
    }

    /**
     * Deletes the Ivy cache directory given by {@link #jarCachePath()}
     */
    default void cleanJarCache() {
        deleteDir(jarCachePath());
    }

    /**
     * Gets a reference to the Ivy jarfile, downloading it if necessary.
     * Calling {@link File#getParent()} on this reference returns the same value as {@link #jarCachePath()}.
     * @return a reference to the Ivy jar.
     */
    default Ivy.Jar ivyJar() {
        Path path = Path.of(jarCachePath(), "ivy.jar");
        if (!path.toFile().exists()) {
            String ivyJarURL = ivyJarURL();
            System.out.print("Downloading " + ivyJarURL + "... ");
            Paths.download(path, ivyJarURL);
            System.out.println("Done");
        }
        return new Ivy.Jar(new File(path));
    }

    /**
     * Requires a library and its dependencies
     * @param depends the required library
     * @return a fileset containing the library and its dependencies
     */
    default Fileset requires(Ivy.Dependency depends) {
        return ivyJar().requires(depends);
    }

    /**
     * Executes a library with a main class
     * @param main an executable dependency
     * @param classpath additional classpath elements
     * @param args command line arguments
     */
    default void exec(Ivy.Executable main, Collection<File> classpath, String... args) {
        ivyJar().execute(main, classpath, args);
    }
}
