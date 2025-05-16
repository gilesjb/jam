#!/usr/bin/env kotlin -Xjvm-default=all -cp build/jam-0.9.jar

interface Example1 : JavaProject {

    override fun sourcePath() = "src/examples/example1"
    override fun buildPath() = "build/examples/example1"

    fun sources() = sourceFiles("**.java")

    fun classes() = javac(sources(), "-d", buildPath("classes"))

    fun hello() = java("-cp", classpath(classes()), "org.copalis.jam.HelloWorld")
}

Project.run(Example1::class.java, Example1::hello, args)
