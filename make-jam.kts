#!/usr/bin/env kotlin -Xjvm-default=all -cp .jam-classes

/*
 * The #! command in this script specifies -classpath .jam-classes
 * because it uses classes bootstrapped by the ./setup script.
 * A build script using a prebuilt Jam library should use a line like:
 *
 * #!/usr/bin/env kotlin -Xjvm-default=all -cp <path of Jam jar>
 */

interface KotlinJamProject : JavaProject, IvyProject {

    fun version() = "0.9"

    fun testDependencies() = resolve("commons-lang:commons-lang:2.1", "commons-cli:commons-cli:1.4")

    fun mainSources() = sourceFiles("main/**.java")

    fun mainClasses() = javac(mainSources(), "-d", buildPath("classes/main"))

    fun testSources() = sourceFiles("test/**.java")

    fun testClasses() = javac(testSources(),
            "-d", buildPath("classes/test"),
            "-cp", classpath(mainClasses(), jUnitLib(), testDependencies()))

    fun testBuild() = junit("test-report", testClasses(), testSources(), mainClasses(), testDependencies())

    fun docs() = javadoc("docs", mainSources())

    fun jarfile() = jar("jam-${version()}.jar", mainSources(), mainClasses())

    fun release() : File {
        testBuild()
        docs()
        return jarfile()
    }
    
    fun about() = "Jam is ready! Run ./make-jam.main.kts to build Jam ${version()}"

    fun viewDocs() {
        System.setProperty("java.awt.headless", "false")
        java.awt.Desktop.getDesktop().browse(File("${docs().base()}/index.html").toURI())
    }
}

Project.run(KotlinJamProject::class.java, KotlinJamProject::release, args)
