import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Test;

public class FileTest {

    File file;

    @Before
    public void setUp() throws URISyntaxException {
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
