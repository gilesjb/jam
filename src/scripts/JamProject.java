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

    default File release() {
        tests();
        docs();
        return jarfile();
    }

    default void viewTestCoverage() throws Exception {
        java.awt.Desktop.getDesktop().browse(coverageReport().toURI());
    }

    default void viewDocs() throws Exception {
        java.awt.Desktop.getDesktop().browse(docs().file("index.html").toURI());
    }

    default void publish() {
        exec("mvn", "install:install-file", "-DgroupId=org.copalis", "-DartifactId=jam",
                "-Dversion=" + version(), "-Dfile=" + release(),
                "-Dpackaging=jar", "-DgeneratePom=true", "-DlocalRepositoryPath=../jam-repo",
                "-DcreateChecksum=true");
    }

    default void examples() {
        jarfile();
        ProcessBuilder pb = new ProcessBuilder().directory(new File("examples/memoizer")).inheritIO();
        exec(pb, "./MemoizerDemo");

        pb.directory(new File("examples/project"));
        exec(pb, "./fibonacci.kts", "fib50");
        exec(pb, "./fibonacci.kts", "clean");
    }

    static void main(String[] args) {
        Project.run(JamProject.class, JamProject::release, args);
    }
}
