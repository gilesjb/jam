package org.copalis.jam;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Utility methods for paths
 *
 * @author gilesjb@gmail.com
 */
public class Paths {
    private Paths() { }

    private static final Path workDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();

    /**
     * Converts a URI to a Path relative to the working directory
     * @param uri the input URI
     * @return the corresponding Path
     */
    public static Path fromURI(URI uri) {
        Path path = Path.of(uri);
        return path.startsWith(workDir) ? workDir.relativize(path) : path;
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

    /**
     * Deletes a directory
     * @param path the location of the directory to delete
     */
    public static void rmDir(Path path) {
        if (Files.exists(path)) {
            try {
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }
                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
