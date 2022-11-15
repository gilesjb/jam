import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.copalis.builder.Timestamped;

/**
 * A reference to an existent file
 */
public class File implements Timestamped, Externalizable {
    private Path path;
    private long modified;

    public File() {
        this.path = null;
        this.modified = 0L;
    }

    public File(Path path) {
        this.path = path;
        try {
            this.modified = Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String read() {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String name() {
        return path.getFileName().toString();
    }

    public boolean isCurrent() {
        try {
            return Files.getLastModifiedTime(path).toMillis() == modified;
        } catch (IOException e) {
            return false;
        }
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(path.toString());
        out.writeLong(modified);
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.path = Path.of(in.readUTF());
        this.modified = in.readLong();
    }

    @Override public String toString() {
        return Objects.toString(path);
    }

    @Override public int hashCode() {
        return Objects.hashCode(path);
    }

    @Override public boolean equals(Object obj) {
        return obj instanceof File && path.equals(((File) obj).path) && modified == ((File) obj).modified;
    }
}
