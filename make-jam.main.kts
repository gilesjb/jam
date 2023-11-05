#!/usr/bin/env kotlin -Xjvm-default=all -cp .jam-classes

interface Maker : JavaProject, IvyProject {

    fun mainSources() = sourceFiles("main/**.java")

    fun mainClasses() = javac("classes/main", mainSources())

    fun testSources() = sourceFiles("test/**.java")

    fun testClasses() = javac("classes/test", testSources(), mainClasses(), jUnitLib())

    fun testBuild() = junit("test-report", testClasses(), testSources(), mainClasses())

    fun docs() = javadoc("docs", mainSources())

    fun jarfile() = jar("jam.jar", mainSources(), mainClasses())

    fun release() {
        testBuild()
        docs()
        jarfile()
    }
}

Project.run(Maker::class.java, Maker::release, args)
