/* Delete this line when using as command line script
#!/usr/bin/java -classpath .jam-classes --source 17
/*
 */

/**
 * The build project for Jam
 *
 * @author gilesjb
 */
public interface JamProject extends JavaProject, IvyProject {

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
        return jar("jam.jar", mainSources(), mainClasses());
    }

    default void release() {
        unitTests();
        docs();
        jarfile();
    }

    static void main(String[] args) {
        Project.run(JamProject.class, JamProject::release, args);
    }
}
