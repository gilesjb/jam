package org.copalis.jam.cli;

import java.io.File;

import org.copalis.jam.memo.Memorizer;

/**
 * Represents the build state
 * @param cacheFile the file used to persist the cache
 * @param memo the memoizer instance
 *
 * @author giles
 */
public record BuildContext(File cacheFile, Memorizer memo) {

    /**
     * A sentinel object that, when used as the return value of a proxied method,
     * will be replaced by {@link BuildController} method interceptor with
     * an instance that references the real cache state
     */
    public static BuildContext REFERENCE = new BuildContext(null, null);
}
