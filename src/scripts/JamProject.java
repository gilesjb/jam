/* Delete this line when using as command line script
#!/usr/bin/java -classpath .jam-classes --source 17
/*
 */

/**
 * The build project for Jam
 *
 * @author gilesjb
 */
public interface JamProject extends JavaProject {

    @Override default File ivyFile() {
        return sourceFile("ivy.xml");
    }

    @Override default Fileset jUnitLib() {
        return ivyConfigurations("test-run");
    }

    default Fileset mainSources() {
        return sourceFiles("main/**.java");
    }

    default Fileset mainClasses() {
        return javaCompile("classes/main", mainSources());
    }

    default Fileset testSources() {
        return sourceFiles("test/**.java");
    }

    default Fileset testClasses() {
        return javaTestCompile("classes/test", testSources(), mainClasses());
    }

    default Fileset testBuild() {
        return jUnit("test-report", testClasses(), testSources(), mainClasses());
    }

    default Fileset docs() {
        return javadoc("docs", mainSources());
    }

    default File jarfile() {
        return jar("jam.jar", mainSources(), mainClasses());
    }

    default void release() {
        testBuild();
        docs();
        jarfile();
    }

    static void main(String[] args) {
        Project.make(JamProject.class, JamProject::release, args);
    }
}
