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

    static String VERSION = "0.9";

    default Fileset testDependencies() {
        return resolve("commons-lang:commons-lang:2.1", "commons-cli:commons-cli:1.4");
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
                mainClasses(), jUnitLib(), testDependencies());
    }

    default Fileset unitTests() {
        return junit("test-report", testClasses(),
                testSources(), mainClasses(), testDependencies());
    }

    default Fileset docs() {
        return javadoc("docs", mainSources());
    }

    default File jarfile() {
        return jar("jam-" + VERSION + ".jar", mainSources(), mainClasses());
    }

    default File release() {
        unitTests();
        docs();
        return jarfile();
    }

    default String about() {
        return "Jam is ready! Run ./make-jam to build Jam " + VERSION;
    }

    static void main(String[] args) {
        Project.run(JamProject.class, JamProject::release, args);
    }
}
