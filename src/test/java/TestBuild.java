/*
#!/usr/bin/java -classpath target/classes --source 19
/*
 */

import java.util.stream.Collectors;

public interface TestBuild extends Project {

    @Override default String buildDir() {
        return "target/test-output";
    }

    default Fileset inputs() {
        return sourceFiles("test/resources/*.txt");
    }

    default File fileCopy(File file) {
        return write("copy/" + file.getName(), "I say " + file.readString().trim());
    }

    default Fileset fileCopies() {
        return inputs().stream().map(this::fileCopy).collect(Fileset.COLLECT);
    }

    default String messages() {
        return fileCopies().stream()
                .map(File::readString)
                .collect(Collectors.joining(", "));
    }

    default File downloadAnt() {
        return download("./ant-bin.jar", "http://archive.apache.org/dist/ant/binaries/apache-ant-1.10.12-bin.zip");
    }

    default File build() {
        return write("message.txt", messages());
    }

    static void main(String[] args) {
        Project.make(TestBuild.class, TestBuild::build, args);
    }
}
