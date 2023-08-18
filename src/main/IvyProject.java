import java.nio.file.Path;

import org.copalis.jam.IvyResolver;

/**
 * Dependency management using Apache Ivy
 *
 * @author gilesjb
 */
public interface IvyProject extends Project {

    /**
     * Specifies the local Ivy cache location
     * @return the path of the Ivy cache
     */
    default String pkgCachePath() {
        return Path.of(System.getProperty("user.home"), ".ivy2").toString();
    }

    /**
     * Override this method to specify the Ivy xml file that defines dependency configurations.
     * @return a reference to the ivy xml file, or null if there is none
     */
    default File ivyFile() {
        return null;
    }

    /**
     * Specifies the Ivy dependency resolver.
     * The Ivy instance uses
     * <ul>
     * <li>the cache directory specified by {@link #pkgCachePath()}
     * <li>the local ivy.xml file specified by {@link #ivyFile()}
     * </ul>
     * @return an instance of Ivy
     */
    default IvyResolver ivyResolver() {
        return new IvyResolver(pkgCachePath(), ivyFile());
    }

    /**
     * Resolves dependency configurations and returns the result
     * @param configurations one or more configuration names
     * @return a fileset referencing the dependency and its transitive dependencies
     */
    default Fileset ivyConfigurations(String... configurations) {
        return ivyResolver().resolveConfigurations(configurations)
                .map(File::new)
                .collect(Fileset.FILES);
    }

    /**
     * Resolves a named dependency
     * @param identifier the dependency name in the form "organization:module:revision"
     * @param configurations optional configuration names
     * @return a fileset referencing the dependency and its transitive dependencies
     */
    default Fileset namedIvyDependency(String identifier, String... configurations) {
        String[] parts = identifier.split(":");
        return ivyResolver().resolveNamedDependency(parts[0], parts[1], parts[2], configurations)
                .map(File::new)
                .collect(Fileset.FILES);
    }

    /**
     * Deletes the Ivy cache directory given by {@link #pkgCachePath()}
     */
    default void cleanPkgCache() {
        rmDir(pkgCachePath());
    }
}
