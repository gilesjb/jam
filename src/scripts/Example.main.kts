#!/usr/bin/env kotlin -Xjvm-default=all -cp jam.jar

interface SimpleProject : JavaProject {

    def sources() = sourceFiles("main/**.java")

    def classes() = javaCompile("classes", sources())

    def jarfile() = jar("jam.jar", classes())
}

Project.make(SimpleProject::class.java, SimpleProject::jarfile, args)
