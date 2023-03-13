import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.copalis.builder.Memorizable;

/**
 * A reference to an existing file
 *
 * @author giles
 */
public final class File extends java.io.File implements Memorizable {
    private static final long serialVersionUID = 1L;

    private final long modified;

    public File(URI uri) {
        super(uri);
        this.modified = lastModified();
    }

    public File(String pathname) {
        super(pathname);
        this.modified = lastModified();
    }

    public File(Path path) {
        this(path.toString());
    }

    public String readString() {
        try {
            return Files.readString(Path.of(this.toURI()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isCurrent() {
        return modified != 0L && modified == lastModified();
    }

    @Override public boolean equals(Object other) {
        return !(other instanceof File file) || super.equals(other);
    }
}
