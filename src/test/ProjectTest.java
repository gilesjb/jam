import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


class ProjectTest {
    public interface FibProject extends Project {

        default long fib(int x) {
            return x < 2 ? x : fib(x - 1) + fib(x - 2);
        }

        default String fib10() {
            return "result=" + fib(10);
        }
    }

    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

    @BeforeEach public void setup() {
        bytes.reset();
        System.setOut(new PrintStream(bytes));
    }

    @AfterEach public void cleanup() {
        Project.run(FibProject.class, FibProject::clean, new String[] {});
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
    }

    @Test public void testTargets() {
        Project.run(FibProject.class, FibProject::fib10, new String[] {"--targets"});
        assertTrue(bytes.toString().contains("fib10"));
    }

    @Test public void testFib() {
        Project.run(FibProject.class, FibProject::fib10, new String[] {});
        String output = bytes.toString();
        assertTrue(output.contains("result=55"));
    }
}
