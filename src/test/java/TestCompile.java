/*
#!/usr/bin/java -classpath target/classes --source 19
/*
 */

public interface TestCompile extends JavaProject {

    default Fileset compile() {
        return javac(sourceFiles("test/java/**.java"), "-verbose",
                "-cp", "target/classes",
                "-d", "target/tmp-classes");
    }

    static void main(String[] args) {
        Project.make(TestCompile.class, TestCompile::compile, args);
    }
}
