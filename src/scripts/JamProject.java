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

    default Fileset testDependencies() {
        return resolve("commons-lang:commons-lang:2.1", "commons-cli:commons-cli:1.4");
    }

    default Fileset mainSources() {
        return sourceFiles("main/**.java");
    }

    default Fileset mainClasses() {
        return javac(mainSources(), "-d", buildPath("classes/main"));
    }

    default Fileset testSources() {
        return sourceFiles("test/**.java");
    }

    default Fileset testClasses() {
        return javac(testSources(),
                "-d", buildPath("classes/test"),
                "-cp", classpath(mainClasses(), jUnitLib(), testDependencies()));
    }

    default Fileset tests() {
        return junit("--scan-classpath", testClasses().base(),
                "-cp", classpath(testClasses(), testSources(), mainClasses(), testDependencies()),
                "--reports-dir=" + buildPath("test-report"));
    }

    default Fileset docs() {
        return javadoc(
                "-sourcepath", mainSources().base(),
                "-d", buildPath("docs"),
                "-subpackages", "", "-quiet");
    }

    default File jarfile() {
        return jar("jam-" + version() + ".jar", mainSources(), mainClasses());
    }

    default File release() {
        tests();
        docs();
        return jarfile();
    }

    default String about() {
        return "Jam is ready! Run ./make-jam to build Jam " + version();
    }

    default void viewDocs() throws Exception {
        java.awt.Desktop.getDesktop().browse(new File(docs().base() + "/index.html").toURI());
    }

    static void main(String[] args) {
        Project.run(JamProject.class, JamProject::release, args);
    }
}
