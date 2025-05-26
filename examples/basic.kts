#!/usr/bin/env kotlin -Xjvm-default=all -cp .jam-classes

interface SimpleKts : JavaProject {

    fun mainSources() = sourceFiles("main/**.java")

    fun mainClasses() = javac("classes/main", mainSources())

    fun docs() = javadoc("docs", "-sourcepath", classpath(mainSources()), "-subpackages", "", "-quiet")

    fun all() {
        docs()
        mainClasses()
    }
}

Project.run(SimpleKts::class.java, SimpleKts::all, args)
