/*
#!/usr/bin/java -classpath target/classes --source 19
/*
 */

public interface TestCompile extends JavaProject {

    @Override default String srcDir() {
        return "src";
    }

    @Override default String buildDir() {
        return "target";
    }

    default File antJar() {
        return download("target/jars/ant-bin.jar", "http://archive.apache.org/dist/ant/binaries/apache-ant-1.10.12-bin.zip");
    }

    default Fileset compile() {
        return javac("test/java", Fileset.find("target/classes", "*.class"),
                "tmp-classes");
    }

    static void main(String[] args) {
        Project.make(TestCompile.class, TestCompile::compile, args);
    }
}
