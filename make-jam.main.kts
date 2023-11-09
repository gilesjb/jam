#!/usr/bin/env kotlin -Xjvm-default=all -cp .jam-classes

/*
 * The #! command in this script specifies -classpath .jam-classes
 * because it uses classes bootstrapped by the ./setup script.
 * A build script using a prebuilt Jam library should use a line like:
 *
 * #!/usr/bin/env kotlin -Xjvm-default=all -cp <path of Jam jar>
 */

object Constants {
    const val VERSION = "0.9"
}

interface Maker : JavaProject, IvyProject {

    fun testDependencies() = resolve("commons-lang:commons-lang:2.1", "commons-cli:commons-cli:1.4")

    fun mainSources() = sourceFiles("main/**.java")

    fun mainClasses() = javac("classes/main", mainSources())

    fun testSources() = sourceFiles("test/**.java")

    fun testClasses() = javac("classes/test", testSources(), mainClasses(), jUnitLib(), testDependencies())

    fun testBuild() = junit("test-report", testClasses(), testSources(), mainClasses(), testDependencies())

    fun docs() = javadoc("docs", mainSources())

    fun jarfile() = jar("jam-${Constants.VERSION}.jar", mainSources(), mainClasses())

    fun release(): File {
        testBuild()
        docs()
        return jarfile()
    }
    
    fun about() = "Jam is ready! Run ./make-jam to build Jam ${Constants.VERSION}"
}

Project.run(Maker::class.java, Maker::release, args)
