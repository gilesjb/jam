#!/usr/bin/env kotlin -Xjvm-default=all -cp jam-classes

interface Maker : JavaProject {

    override fun unitTestLibrary() =
        Ivy.configuredDependency(sourceFile("ivy.xml"), "test")
                .mainClass("org.junit.runner.JUnitCore")

    fun mainSources() = sourceFiles("main/**.java")

    fun mainClasses() = javaCompile("classes/main", mainSources())

    fun testSources() = sourceFiles("test/**.java")

    fun testClasses() = javaTestCompile("classes/test", testSources(), mainClasses())

    fun testBuild() = jUnit(testClasses(), testSources(), mainClasses())

    fun docs() = javadoc("docs", mainSources())

    fun jarfile() = jar("jam.jar", mainSources(), mainClasses())

    fun release() {
        testBuild()
        docs()
        jarfile()
    }
}

Project.make(Maker::class.java, Maker::release, args)
