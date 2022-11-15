import java.util.List;
import java.util.stream.Collectors;

public interface TestProject extends Builder {

    @Override default String buildPath() {
        return "target/test-output";
    }

    default File fileCopy(File file) {
        return write("copy/" + file.name(), "I say " + file.read().trim());
    }

    default List<File> fileCopies() {
        return sources("test/resources/*.txt").stream()
                .map(this::fileCopy).toList();
    }

    default String concatenatedMessage() {
        return fileCopies().stream()
                .map(File::read)
                .collect(Collectors.joining(", "));
    }

    default File build() {
        return write("message.txt", concatenatedMessage());
    }

    static void main(String[] args) {
        Builder.make(TestProject.class, TestProject::build, args);
    }
}
