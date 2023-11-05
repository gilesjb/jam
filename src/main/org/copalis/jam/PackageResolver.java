package org.copalis.jam;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * A package dependency resolver that fetches packages from a repository
 *
 * @author giles
 */
public interface PackageResolver extends Serializable {
    /**
     * Resolves dependencies
     * @param dependencies dependency identifiers in the format org:name:rev
     * @return a stream of Path objects referencing the resolved dependencies
     */
    public Stream<Path> resolve(String... dependencies);

    /**
     * Deletes the package cache
     */
    public void cleanCache();
}
