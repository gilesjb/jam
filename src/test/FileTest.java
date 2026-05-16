import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URISyntaxException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FileTest {

    File file;
    Serializable fileState;

    @BeforeEach
    void test() throws URISyntaxException {
        file = new File(getClass().getClassLoader().getResource("file.txt").toURI());
        fileState = file.currentState();
    }

    @Test
    public void testCurrent() {
        assertFalse(file.modifiedSince(fileState));
    }

    @Test
    public void testSerializable() throws IOException {
        try (ByteArrayOutputStream tmp = new ByteArrayOutputStream()) {
            try (ObjectOutputStream out = new ObjectOutputStream(tmp)) {
                out.writeObject(file);
            }
        }
    }
}
