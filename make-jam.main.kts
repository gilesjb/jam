#!/usr/bin/env kotlin -Xjvm-default=all -cp .jam-classes

interface Maker : JavaProject, IvyProject {

    fun testDependencies() = resolve("commons-lang:commons-lang:2.1", "commons-cli:commons-cli:1.4")

    fun mainSources() = sourceFiles("main/**.java")

    fun mainClasses() = javac("classes/main", mainSources())

    fun testSources() = sourceFiles("test/**.java")

    fun testClasses() = javac("classes/test", testSources(), mainClasses(), jUnitLib(), testDependencies())

    fun testBuild() = junit("test-report", testClasses(), testSources(), mainClasses(), testDependencies())

    fun docs() = javadoc("docs", mainSources())

    fun jarfile() = jar("jam.jar", mainSources(), mainClasses())

    fun release() {
        testBuild()
        docs()
        jarfile()
    }
}

Project.run(Maker::class.java, Maker::release, args)
