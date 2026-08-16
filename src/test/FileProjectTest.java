import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

class FileProjectTest {

    public interface FileReader extends FileProject {
        default String fileContents() {
            log.add("reading file");
            return read("test/file.txt");
        }
    }

    static List<String> log;
    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

    @BeforeEach public void setup() {
        bytes.reset();
        System.setOut(new PrintStream(bytes));
        Project.run(FileReader.class, FileReader::clean, new String[] {});
        log = new LinkedList<>();
    }

    @AfterEach public void cleanup() {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
    }

//    @Test // currently disabled because it breaks Jypeter
    public void testFileModification() throws URISyntaxException, IOException {
        Project.run(FileReader.class, FileReader::fileContents, new String[] {});
        assertEquals(1, log.size());

        File file = new File("src/test/file.txt");

        long lastModified = file.lastModified();
        file.setLastModified(lastModified + 1);
        long nowModified = file.lastModified();

        assertNotEquals(lastModified, nowModified);

        Project.run(FileReader.class, FileReader::fileContents, new String[] {});
        assertEquals(2, log.size());
    }
}
