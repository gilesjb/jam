/*
#!/usr/bin/java -classpath classes --source 19
/*
 */

public interface Compile extends JavaProject {

    @Override default String sourcePath() {
        return "src";
    }

    @Override default String buildPath() {
        return "built";
    }

    default Fileset jars() {
        return Fileset.of(download("jars/junit.jar",
                "https://repo1.maven.org/maven2/junit/junit/4.13.2/junit-4.13.2.jar"));
    }

    default Fileset mainClasses() {
        return javaCompile("classes", sourceFiles("main/**.java"));
    }

    default Fileset testClasses() {
        return javaCompile("test-classes", sourceFiles("test/**.java"), mainClasses(), jars());
    }

    default Fileset build() {
        return testClasses();
    }

    static void main(String[] args) {
        Project.make(Compile.class, Compile::build, args);
    }
}
