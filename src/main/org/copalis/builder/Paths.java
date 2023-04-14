package org.copalis.builder;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
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

    /**
     * Downloads a resource
     * @param path the download path
     * @param url the resource to download
     */
    public static void download(Path path, String url) {
        try {
            Files.createDirectories(path.getParent());
            try (ReadableByteChannel readableByteChannel = Channels.newChannel(new URL(url).openStream());
                    FileOutputStream out = new FileOutputStream(path.toFile())) {
                out.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
