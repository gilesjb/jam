/* Delete this line when using as command line script
#!/usr/bin/env -S java -classpath .jam-classes --source 23
/*
 * The #! command in this file specifies -classpath .jam-classes
 * because it uses classes bootstrapped by the ./setup script.
 * A build script using a prebuilt Jam library should use a line like:
 *
 * #!/usr/bin/java -classpath <path of Jam jar> --source 17
 */

/**
 * The build project for Jam.
 *
 * @author gilesjb
 */
public interface JamProject extends JavaProject {

    default String version() {
        return "0.9.1";
    }

    default Fileset mainSources() {
        return sourceFiles("main/**.java");
    }

    default Fileset mainClasses() {
        return javac("classes/main", mainSources(),
                "--release", "17");
    }

    default Fileset testSources() {
        return sourceFiles("test/**.java");
    }

    default Fileset testClasses() {
        return javac("classes/test", testSources(),
                "-cp", classpath(mainClasses(), jUnitLib()));
    }

    default Fileset tests() {
        Fileset agent = resolve("org.jacoco:org.jacoco.agent:0.8.13#runtime");
        return junit("tests/report",
                "-javaagent:" + agent + "=destfile=" + buildPath("tests/report") + "/jacoco.exec",
                "--scan-classpath", classpath(testClasses()),
                "-cp", classpath(testClasses(), testSources(), mainClasses()));
    }

    default File coverageReport() {
        String report = buildPath("tests/coverage");
        java("-jar", resolve("org.jacoco:org.jacoco.cli:0.8.13#nodeps").toString(),
                "report", tests().file("jacoco.exec").getPath(),
                "--classfiles", classpath(mainClasses()),
                "--sourcefiles", classpath(mainSources()),
                "--html", report);
        return new File(report + "/index.html");
    }

    default Fileset docs() {
        return javadoc("docs",
                "-sourcepath", classpath(mainSources()),
                "-subpackages", "",
                "-quiet",
                "-notimestamp");
    }

    default File jarfile() {
        return jar("jam-" + version() + ".jar", mainClasses(), mainSources());
    }

    default File build() {
        tests();
        docs();
        return jarfile();
    }

    default String releasePath(String type) {
        buildPath("release/org/copalis/jam/" + version());
        return "release/org/copalis/jam/" + version() + "/jam-" + version() + type;
    }

    default void releaseArtifacts() {
        jar(releasePath(".jar"), mainClasses(), mainSources());
        jar(releasePath("-sources.jar"), mainSources());
        jar(releasePath("-javadoc.jar"), docs());
        write(releasePath(".pom"),
                read("release/pom.xml").replace("{version}", version()));
        write("jreleaser.yml",
                read("release/jreleaser.yml").replace("{version}", version()));
    }

    default void viewTestCoverage() throws Exception {
        java.awt.Desktop.getDesktop().browse(coverageReport().toURI());
    }

    default void viewDocs() throws Exception {
        java.awt.Desktop.getDesktop().browse(docs().file("index.html").toURI());
    }

    static void main(String[] args) {
        Project.run(JamProject.class, JamProject::build, args);
    }
}
