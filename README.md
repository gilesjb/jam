# Jam - a build tool

## An example build script

Let's say we create a Java [script](https://openjdk.org/jeps/330) called `jam-build`:

```java
#!/usr/bin/java -classpath jam.jar --source 14

public interface ExampleProject extends JavaProject {
    default Fileset sources() {
        return sourceFiles("main/**.java");
    }

    default Fileset classes() {
        return javaCompile("classes", sources());
    }

    default File jarfile() {
        return jar("example.jar", classes());
    }

    static void main(String[] args) {
        Project.make(ExampleProject.class, ExampleProject::jarfile, args);
    }
}
```

The first line of the script tells Java that `jam.jar` is on the classpath.

The `main()` method tells Jam to execute `ExampleProject` with `ExampleProject::jarfile` as the default target method.

When we run the script with no arguments Jam executes the default target,
displaying the call graph of the methods that it calls:

```console
% ./jam-build
[execute] jarfile
[execute]   classes
[execute]     sources
[execute]       sourceFiles 'main/**.java'
[execute]         sourcePath
[execute]     javaCompile 'classes' src/main/**.java
[execute]       buildPath
[execute]       javac src/main/**.java '-d' 'build/classes'
[execute]   jar 'example.jar' build/classes/**.class
[current]     buildPath
Result: build/example.jar
COMPLETED in 365ms
```
This builds the file `build/example.jar`.

Jam *memoizes* method calls and caches their return values.
Notice that the second call to `buildPath()` is labeled as `[current]`.
This means the method had already been executed, 
so Jam used the cached value rather than executing it again. 

Jam persists its cache between runs,
as we can see if we run the build again:

```console
% ./jam-build
>[current] jarfile
Result: build/example.jar
COMPLETED in 50ms
```

The result of `jarfile()` was already in the cache,
so Jam skipped its execution.

If the timestamps of source files have changed Jam will invalidate the cache entries for those files
and also the cached results of any methods that *depended on them*,
triggering a rebuild of affected build artifacts.

```console
% touch src/main/*.java
% ./jam-build
[update ] jarfile
[update ]   classes
[update ]     sources
[update ]       sourceFiles 'main/**.java'
[current]         sourcePath
[update ]     javaCompile 'classes' src/main/**.java
[current]       buildPath
[execute]       javac src/main/**.java '-d' 'build/classes'
[update ]   jar 'example.jar' build/classes/**.class
COMPLETED in 332ms
```

Any method with 0 parameters is a *target* that can be specified on the command line.
The full set of targets can be displayed by running the `targets()` method which is inherited from the parent interface:

```console
% ./jam-build targets
[execute] targets
Project targets
  classes : Fileset
  sources : Fileset
  jarfile : File
  clean : void
  targets : void
  help : void
  buildPath : String
  sourcePath : String
COMPLETED in 15ms
```

Among the inherited targets is a `clean()` method that deletes the build directory and the cache:

```console
% ./jam-build clean
[execute] clean
[execute]   deleteBuildDir ''
[current]     buildPath
COMPLETED in 36ms
```

## Things to know

You may have noticed that the script has no `import` statements.
This is because types provided by Jam are declared in the default package so they don't need to be imported.

Source file paths are relative to the base directory `src`,
and the base directory for build artifacts is `build`.
To change these override the `sourcePath()` and `buildPath()` methods.
The Jam cache file is stored in the build directory.

Jam follows these conventions:
* `void` methods are not memoized, and should be used for logic that has side effects
* Non-void methods with 1 or more parameters are memoized, but cannot be specified as targets

Due to how Jam's memoizer is implemented, a project must follow these rules:
* The project class must be an interface containing `default` methods
* All parameters and return types must be primitive or serializable

## Kotlin scripts

Jam scripts can be written in Kotlin too.
In fact Jam should work with any JVM language that supports default methods.

Written in Kotlin, the previous script looks like this:

```kotlin
#!/usr/bin/env kotlin -Xjvm-default=all -cp jam.jar

interface ExampleProject : JavaProject {

    def sources() = sourceFiles("main/**.java")

    def classes() = javaCompile("classes", sources())

    def jarfile() = jar("example.jar", classes())
}

Project.make(ExampleProject::class.java, ExampleProject::jarfile, args)
```

## Building the Jam library

Building `jam.jar` requires JDK 14 or higher.

1. Compile the main classes by running `./setup`
2. Run either `./make-jam` (Java) or `./make-jam.main.kts` (Kotlin) to compile Jam, run unit tests, and create `jam.jar`

## Status

Jam is currently in Alpha and everything about it is subject to the whims of the author.
