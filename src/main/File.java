import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.copalis.jam.memo.Mutable;

/**
 * A reference to an existing file
 *
 * @author gilesjb
 */
public final class File extends java.io.File implements Mutable {
    private static final long serialVersionUID = 1L;

    /**
     * The modified time of the file
     */
    private final long modified;

    /**
     * Creates a reference to a file specified by a URI
     * @param uri the file's URI
     */
    public File(URI uri) {
        super(uri);
        this.modified = lastModified();
    }

    /**
     * Creates a reference to a file specified by a path string
     * @param pathname the path string
     */
    public File(String pathname) {
        super(pathname);
        this.modified = lastModified();
    }

    /**
     * Creates a reference to a file specified by a Path object
     * @param path the path to the file
     */
    public File(Path path) {
        this(path.toString());
    }

    /**
     * Generates a digest of the file's contents
     * @param algorithm the message digest algorithm
     * @return a hexadecimal string representation of the file's message digest
     * @see MessageDigest#getInstance(String)
     */
    public String digest(String algorithm) {
        try (FileInputStream fis = new FileInputStream(this);
                BufferedInputStream bis = new BufferedInputStream(fis)) {

            MessageDigest md = MessageDigest.getInstance(algorithm);

            byte[] buffer = new byte[8192]; // 8KB buffer
            int bytesRead;

            while ((bytesRead = bis.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }

            byte[] digest = md.digest();
            return IntStream.range(0, digest.length)
                    .mapToObj(i -> String.format("%02x", digest[i]))
                    .collect(Collectors.joining());

        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean modified() {
        return modified == 0L || modified != lastModified();
    }

    @Override public boolean equals(Object other) {
        return !(other instanceof File file) || super.equals(other);
    }
}
