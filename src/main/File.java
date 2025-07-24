import java.net.URI;
import java.nio.file.Path;

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

    public boolean modified() {
        return modified == 0L || modified != lastModified();
    }

    @Override public boolean equals(Object other) {
        return !(other instanceof File file) || super.equals(other);
    }
}
