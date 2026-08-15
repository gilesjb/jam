import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;

class FileProjectTest {
    static List<String> log;

    public interface TestProject extends FileProject {
        default String fileContents() {
            log.add("reading file");
            return read("test/file.txt");
        }

        default void testFile() {
            assertEquals("hello", fileContents());
        }
    }

    @BeforeEach public void setup() {
        Project.run(TestProject.class, TestProject::clean, new String[] {});
        log = new LinkedList<>();
    }

//    @Test
    void testFileModification() throws URISyntaxException, IOException {
//        Project.run(TestProject.class, TestProject::testFile, new String[] {});
//        assertEquals(1, log.size());
//
//        File file = new File("src/test/file.txt");
//
//        long lastModified = file.lastModified();
//        file.setLastModified(lastModified + 1);
//        long nowModified = file.lastModified();
//
//        assertNotEquals(lastModified, nowModified);
//
//        Project.run(TestProject.class, TestProject::fileContents, new String[] {});
//        assertEquals(2, log.size());
    }
}
