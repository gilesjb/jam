#!/usr/bin/env kotlin -Xjvm-default=all -cp jam.jar

interface ExampleProject : JavaProject {

    def sources() = sourceFiles("main/**.java")

    def classes() = javaCompile("classes", sources())

    def jarfile() = jar("example.jar", classes())
}

Project.make(ExampleProject::class.java, ExampleProject::jarfile, args)
