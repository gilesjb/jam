import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.copalis.builder.Checked;

public class File extends java.io.File implements Checked {
    private static final long serialVersionUID = 1L;

    private final long modified;

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
        return modified != 0L && modified == new File(getPath()).lastModified();
    }

    @Override public boolean equals(Object other) {
        return !(other instanceof File file) || super.equals(other) && modified == file.modified;
    }
}
