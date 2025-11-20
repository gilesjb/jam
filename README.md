
# Jam build tool

**Jam** is a library designed to be used in Java or Kotlin command-line scripts,
especially scripts for build automation.

There are 3 parts to Jam:
1. A dependency-aware method memoizer
2. A script controller that handles command-line arguments
3. A library of utility functions for compiling code

## Jam in action

### Jam scripts

Here is an example build script written in Kotlin. Kotlin scripts must have names that end with `.main.kts`.
This file is called `hello.main.kts`.

```kotlin
#!/usr/bin/env kotlin -Xjvm-default=all
@file:Repository("https://raw.githubusercontent.com/gilesjb/jam-repo/refs/heads/main")
@file:DependsOn("org.copalis:jam:0.9.1")

interface HelloWorld : JavaProject {

    fun worldStr() = "World"

    fun worldName() = read("world.txt")

    fun greet(place : String) = println("Hello, ${place}!")

    fun helloJava() = sourceFiles("HelloWorld.java")

    fun helloClasses() = classpath(javac("classes", helloJava()))

    fun runHello() {
        java("-cp", helloClasses(), "HelloWorld")
    }

    fun printHellos() {
        greet(worldStr())
        greet(worldName())
        runHello()
    }
}

Project.run(HelloWorld::class.java, HelloWorld::printHellos, args)

```

The script can be run directly from the command line.
It just requires Kotlin to be installed; the Jam library will be downloaded automatically.

Passing the `--help` option displays options:
<img src='docs/pics/00.png' width='1000'>

### Build targets

Specifying `--targets` shows the build targets
<img src='docs/pics/01.png' width='1000'>
Targets are just project methods that have 0 arguments.
The target listing shows method defined by the script's `HelloWorld` interface and inherited from its parent interfaces.

Let's run the `worldStr` target:
<img src='docs/pics/02.png' width='1000'>
The output log shows that `worldStr()` was executed and returned the value "World".
 
Another target is `worldName`
<img src='docs/pics/03.png' width='1000'>
Now the output log shows that `worldName()` was executed, and it in turn called `read("world.text")`

### Result caching

If we look at the targets again we can see that both `worldStr` and `worldName` targets are tagged as **fresh**.
This means that their results are cached and up to date.
<img src='docs/pics/04.png' width='1000'>

The cache contents can be viewed with the `--cache` option.
<img src='docs/pics/05.png' width='1000'>
(Notice that the results cache is stored in a hidden file, and its name is derived from the project name.)

Because its result is cached, if we run `worldName` again the result will be fetched from cache.
<img src='docs/pics/06.png' width='1000'>

### Dependency tracking

Jam is able to infer that the result of `worldName` depends on the contents of the file `src/world.txt`.
This means that if the last-modified time of that file changes, the cached result will be invalidated.

<img src='docs/pics/07.png' width='1000'>

Now the `worldName` target is shown as stale.
<img src='docs/pics/08.png' width='1000'>

Executing the target again it will be rebuilt.
<img src='docs/pics/09.png' width='1000'>

### Compiling code

The `JavaProject` interface provides a variety of methods for building and executing Java code.
This is demonstrated by the `runHello` target:
<img src='docs/pics/10.png' width='1000'>

Jam stores references to the compiled classes.
<img src='docs/pics/11.png' width='1000'>

But if there are modifications to source files,
<img src='docs/pics/12.png' width='1000'>

then Jam will recompile the classes.
<img src='docs/pics/13.png' width='1000'>
