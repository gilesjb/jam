package org.copalis.builder;

import java.nio.file.Path;

/**
 * Utility methods for paths
 *
 * @author giles
 */
public class Paths {
    private Paths() { }

    /**
     * Creates a Path object from the concatenation of two string paths
     * @param base the base path
     * @param path a path relative to the base
     * @return a new Path
     */
    public static Path join(String base, String path) {
        return Path.of(base).resolve(path);
    }
}
