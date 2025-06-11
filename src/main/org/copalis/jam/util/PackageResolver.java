package org.copalis.jam.util;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * A package dependency resolver which fetches packages from a repository
 *
 * @author giles
 */
public interface PackageResolver extends Serializable {
    /**
     * Resolves dependencies
     * @param dependencies dependency identifiers in the format {@code "org:name:revision"}.
     * If org and name are the same, the format {@code "name:revision"} can be used instead.
     * @return a stream of Path objects referencing the resolved dependencies
     */
    public Stream<Path> resolve(String... dependencies);

    /**
     * Deletes the package cache
     */
    public void cleanCache();
}
