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
        return junit("test-report",
                "--scan-classpath", classpath(testClasses()),
                "-cp", classpath(testClasses(), testSources(), mainClasses(), testLibs()));
    }

    default Fileset docs() {
        return javadoc("docs",
                "-sourcepath", classpath(mainSources()),
                "-subpackages", "");
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
        java.awt.Desktop.getDesktop().browse(docs().file("index.html").toURI());
    }

    static void main(String[] args) {
        Project.run(JamProject.class, JamProject::release, args);
    }
}
