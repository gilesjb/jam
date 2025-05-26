#!/usr/bin/env kotlin -Xjvm-default=all -cp .jam-classes

interface BasicProject : JavaProject {

    fun mainSources() = sourceFiles("main/**.java")

    fun mainClasses() = javac("classes/main", mainSources())

    fun docs() = javadoc("docs", "-sourcepath", classpath(mainSources()), "-subpackages", "")
    
    fun all() {
        docs()
        mainClasses()
    }
}

Project.run(BasicProject::class.java, BasicProject::all, args)
