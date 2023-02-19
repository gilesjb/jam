/*
#!/usr/bin/java -classpath target/classes --source 19
/*
 */

public interface TestCompile extends JavaProject {

    default File antJar() {
        return download("./ant-bin.jar", "http://archive.apache.org/dist/ant/binaries/apache-ant-1.10.12-bin.zip");
    }

    default Fileset compile() {
        return javac(sourceFiles("test/java/**.java"), "-verbose",
                "-cp", "target/classes",
                "-d", "target/tmp-classes");
    }

    static void main(String[] args) {
        Project.make(TestCompile.class, TestCompile::compile, args);
    }
}
