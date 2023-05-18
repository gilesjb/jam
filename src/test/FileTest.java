import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URISyntaxException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FileTest {

    File file;

    @BeforeEach
    void test() throws URISyntaxException {
        file = new File(getClass().getClassLoader().getResource("file.txt").toURI());
    }

    @Test
    public void testCurrent() {
        assertTrue(file.current());
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
