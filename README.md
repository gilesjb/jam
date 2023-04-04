# Jam - a build tool

## Basic usage

Let's say we create a Java script called `make-simple`:

```java
#!/usr/bin/java -classpath jam.jar --source 14

public interface SimpleProject extends JavaProject {
    default Fileset sources() {
        return sourceFiles("main/**.java");
    }

    default Fileset classes() {
        return javaCompile("classes", sources());
    }

    default File jarfile() {
        return jar("jam.jar", classes());
    }

    static void main(String[] args) {
        Project.make(SimpleProject.class, SimpleProject::jarfile, args);
    }
}
```

Note the first line of the script specifies that `jam.jar` is on the classpath.
The `main()` method in this script calls the Jam library specifying `jarfile()` method as the default target method.

If we run the script with no parameters, Jam executes the default target,
displaying the call graph of the methods that are executed:

```console
% ./make-simple
[execute] jarfile
[execute]   classes
[execute]     sources
[execute]       sourceFiles 'main/**.java'
[execute]         sourcePath
[execute]     javaCompile 'classes' src/main/**.java
[execute]       buildPath
[execute]       javac src/main/**.java '-d' 'build/classes'
[execute]   jar 'jam.jar' build/classes/**.class
[current]     buildPath
COMPLETED in 365ms
```

Jam intercepts method calls and *memoizes* their return values.
Notice that the second call to `buildPath()` is marked as `[current]`.
This means Jam found a return value for the method in its cache and returned that to the calling method, short-circuiting the call. 

Jam's cache is saved to disk,
as we can see if we run the script again.

```console
% ./make-simple
>[current] jarfile
COMPLETED in 50ms
```

If source files are modified Jam will invalidate the cache entries for those files or anything derived from them.

```console
% touch src/main/*.java
% ./make-simple
[update ] jarfile
[update ]   classes
[update ]     sources
[update ]       sourceFiles 'main/**.java'
[current]         sourcePath
[update ]     javaCompile 'classes' src/main/**.java
[current]       buildPath
[execute]       javac src/main/**.java '-d' 'build/classes'
[update ]   jar 'jam.jar' build/classes/**.class
COMPLETED in 332ms
```

Because the build script interface extends `JavaProject` it inherits a `clean()` method which can be specified as a target:

```console
% ./make-simple clean
[execute] clean
[execute]   deleteBuildDir ''
[current]     buildPath
COMPLETED in 36ms
```

There is also a `targets()` method which prints all the targets and their return types:

```console
% ./make-simple targets
[execute] targets<b>
Project targets
  classes : Fileset
  sources : Fileset
  jarfile : File
  clean : void
  targets : void
  help : void
  buildPath : String
  sourcePath : String</b>
COMPLETED in 15ms
```

## Building the Jam library

This repo does not use any standard build tools like Ant or Maven.
In fact, it builds itself.
To build this library and produce `jam.jar` requires two steps

1. Bootstrap the main classes by running `./setup`
2. Run either the `./make-jam` (Java) or `./make-jam.main.kts` (Kotlin) build script to compile Jam, run unit tests, and package everything in `jam.jar`
