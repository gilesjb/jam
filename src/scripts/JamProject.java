/* Delete this line when using as command line script
#!/usr/bin/java -classpath .jam-classes --source 17
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
public interface JamProject extends JavaProject, IvyProject {

    default String version() {
        return "0.9";
    }

    default Fileset testLibs() {
        return resolve("commons-lang:2.1", "commons-cli:1.4");
    }

    default Fileset mainSources() {
        return sourceFiles("main/**.java");
    }

    default Fileset mainClasses() {
        return javac("classes/main", mainSources());
    }

    default Fileset testSources() {
        return sourceFiles("test/**.java");
    }

    default Fileset testClasses() {
        return javac("classes/test", testSources(),
                "-cp", classpath(mainClasses(), jUnitLib(), testLibs()));
    }

    default Fileset tests() {
        return junit("tests/report",
                "-javaagent:" + resolve("org.jacoco:org.jacoco.agent:0.8.9#runtime")
                        + "=destfile=" + buildPath("tests/report") + "/jacoco.exec",
                "--scan-classpath", classpath(testClasses()),
                "-cp", classpath(testClasses(), testSources(), mainClasses(), testLibs()));
    }

    default File coverageReport() {
        String report = buildPath("tests/coverage");
        java("-jar", resolve("org.jacoco:org.jacoco.cli:0.8.9#nodeps").toString(),
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
                "-quiet");
    }

    default File jarfile(String post, Fileset... contents) {
        String name = "jam-" + version() + post + ".jar";
        File jarFile = jar(name, contents);
        write(name + ".md5", jarFile.digest("MD5"));
        write(name + ".sha1", jarFile.digest("SHA1"));
        return jarFile;
    }

    default File release() {
        tests();
        jarfile("-javadoc", docs());
        jarfile("-sources", mainSources());
        return jarfile("", mainClasses());
    }

    default String about() {
        return "Jam is ready! Run ./make-jam to build Jam " + version();
    }

    default void viewTestCoverage() throws Exception {
        java.awt.Desktop.getDesktop().browse(coverageReport().toURI());
    }

    default void viewDocs() throws Exception {
        java.awt.Desktop.getDesktop().browse(docs().file("index.html").toURI());
    }

    static void main(String[] args) {
        Project.run(JamProject.class, JamProject::release, args);
    }
}
