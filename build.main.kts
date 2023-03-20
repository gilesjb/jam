#!/usr/bin/env kotlin -Xjvm-default=all -cp classes

interface Maker : JavaProject {

    override fun buildPath() = "build-kt"
    
    fun mavenJar(org: String, name: String, version: String) =
        download("jars/$name.jar", "https://repo1.maven.org/maven2/$org/$name/$version/$name-$version.jar")

    fun testJars() = Fileset.of(
                mavenJar("junit", "junit", "4.13.2"),
                mavenJar("org/hamcrest", "hamcrest-core", "1.3"))

    fun mainSources() = sourceFiles("main/**.java")

    fun mainClasses() = javaCompile("classes/main", mainSources())

    fun testSources() = sourceFiles("test/**.java")

    fun testClasses() = javaCompile("classes/test", testSources(), mainClasses(), testJars())

    fun testBuild() {
        junit(testClasses(), testSources(), mainClasses(), testJars())
    }

    fun docs() = javadoc("docs", mainSources(), "org.copalis.builder")

    fun release() {
        testBuild()
        docs()
        jar(".jam.jar", mainSources(), mainClasses())
    }
}

Project.make(Maker::class.java, Maker::release, args)
