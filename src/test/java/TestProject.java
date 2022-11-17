//#!/usr/bin/java -classpath target/classes --source 19

import java.util.List;
import java.util.stream.Collectors;

public interface TestProject extends Project {

    @Override default String buildDir() {
        return "target/test-output";
    }

    default File fileCopy(File file) {
        return write("copy/" + file.name(), "I say " + file.read().trim());
    }

    default List<File> fileCopies() {
        return sourceFiles("test/resources/*.txt").stream()
                .map(this::fileCopy).toList();
    }

    default String messages() {
        return fileCopies().stream()
                .map(File::read)
                .collect(Collectors.joining(", "));
    }

    default File build() {
        return write("message.txt", messages());
    }

    static void main(String[] args) {
        Project.make(TestProject.class, TestProject::build, args);
    }
}
