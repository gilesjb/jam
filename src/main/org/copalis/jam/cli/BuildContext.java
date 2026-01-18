package org.copalis.jam.cli;

import java.io.File;

import org.copalis.jam.memo.Memorizer;

/**
 * Represents the build state
 * @param memoizer the memoizer instance
 * @param cacheFile the file used to persist the cache
 *
 * @author giles
 */
public record BuildContext(Memorizer memoizer, File cacheFile) {

    /**
     * A sentinel object that, when used as the return value of a proxied method,
     * will be replaced by {@link BuildController} method interceptor with
     * an instance that references the real cache state
     */
    public static BuildContext REFERENCE = new BuildContext(null, null);
}
