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

    default File mavenJar(String org, String name, String version) {
        return download(
                String.format("jars/%s.jar", name),
                String.format("https://repo1.maven.org/maven2/%s/%s/%s/%2$s-%3$s.jar", org, name, version));
    }

    default Fileset testJars() {
        return Fileset.of(
                mavenJar("junit", "junit", "4.13.2"),
                mavenJar("org/hamcrest", "hamcrest-core", "1.3"));
    }

    default Fileset mainClasses() {
        return javaCompile("classes/main", sourceFiles("main/**.java"));
    }

    default Fileset testClasses() {
        return javaCompile("classes/test", sourceFiles("test/**.java"), mainClasses(), testJars());
    }

    default Fileset build() {
        return testClasses();
    }

    default void testBuild() {
        junit(testClasses(), sourceFiles("test/**.txt"), mainClasses(), testJars());
    }

    static void main(String[] args) {
        Project.make(Compile.class, Compile::testBuild, args);
    }
}
