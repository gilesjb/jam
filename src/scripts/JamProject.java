/* Delete this line when using as command line script
#!/usr/bin/env -S java -ea -classpath .jam-classes --source 23
/*
 * The #! command in this file specifies -classpath .jam-classes
 * because it uses classes bootstrapped by the ./setup script.
 * A Java build script using a prebuilt Jam library should use a line like:
 *
 * #!/usr/bin/env -S java -classpath <path of Jam jar> --source 17
 */

/**
 * The build project for Jam.
 *
 * @author gilesjb
 */
public interface JamProject extends JavaProject {

    default String version() {
        return "0.9.2";
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

    default String releasePath() {
        return "release/org/copalis/jam/" + version();
    }

    default String releaseArtifact(String type) {
        return releasePath() + "/jam-" + version() + type;
    }

    default File sign(File file) {
        File asc;
        while (!(asc = new File(file.toString() + ".asc")).exists()) {
            exec("gpg", "--armor", "--detach-sign", "--batch", file.toString());
        }
        return asc;
    }

    default String releaseArtifacts() {
        buildPath(releasePath());
        sign(jar(releaseArtifact(".jar"), mainClasses(), mainSources()));
        sign(jar(releaseArtifact("-sources.jar"), mainSources()));
        sign(jar(releaseArtifact("-javadoc.jar"), docs()));
        sign(write(releaseArtifact(".pom"), read("release/pom.xml").replace("{version}", version())));
        return releasePath();
    }

    default void publish() {
        assert System.getenv("SONATYPE_USER") != null : "SONATYPE_USER must be set";
        assert System.getenv("SONATYPE_PASS") != null : "SONATYPE_PASS must be set";

        java("-jar", ((org.copalis.jam.util.IvyResolver) packageResolver()).ivyJar().toString(),
                "-publish", "maven-publish",
                "-ivy", sourceFile("release/ivy.xml").toString(),
                "-settings", sourceFile("release/ivysettings.xml").toString(),
                "-DVERSION=" + version(),
                "-DARTIFACT_PATH=" + releaseArtifacts());
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
