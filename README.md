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

<pre>
% ./make-simple
<span style="color:#aa0">[execute]</span> jarfile
<span style="color:#aa0">[execute]</span>   classes
<span style="color:#aa0">[execute]</span>     sources
<span style="color:#aa0">[execute]</span>       sourceFiles 'main/**.java'
<span style="color:#aa0">[execute]</span>         sourcePath
<span style="color:#aa0">[execute]</span>     javaCompile 'classes' src/main/**.java
<span style="color:#aa0">[execute]</span>       buildPath
<span style="color:#aa0">[execute]</span>       javac src/main/**.java '-d' 'build/classes'
<span style="color:#aa0">[execute]</span>   jar 'jam.jar' build/classes/**.class
<span style="color:#0a0">[current]</span>     buildPath
<span style="color:#0d0">COMPLETED in 365ms</span>
</pre>

Jam intercepts method calls and *memoizes* their return values.
Notice that the second call to `buildPath()` is marked as `[current]`.
This means Jam found a return value for the method in its cache and returned that to the calling method, short-circuiting the call. 

Jam's cache is saved to disk,
as we can see if we run the script again.

<pre>
% ./make-simple
<span style="color:green">[current]</span> jarfile
<span style="color:#0d0">COMPLETED in 50ms</span>
</pre>

If source files are modified Jam will invalidate the cache entries for those files or anything derived from them.

<pre>
% touch src/main/*.java
% ./make-simple
<span style="color:#0aa">[update ]</span> jarfile
<span style="color:#0aa">[update ]</span>   classes
<span style="color:#0aa">[update ]</span>     sources
<span style="color:#0aa">[update ]</span>       sourceFiles 'main/**.java'
<span style="color:#0a0">[current]</span>         sourcePath
<span style="color:#0aa">[update ]</span>     javaCompile 'classes' src/main/**.java
<span style="color:#0b0">[current]</span>       buildPath
<span style="color:#aa0">[execute]</span>       javac src/main/**.java '-d' 'build/classes'
<span style="color:#0aa">[update ]</span>   jar 'jam.jar' build/classes/**.class
<span style="color:#0d0">COMPLETED in 332ms</span>
</pre>

Because the build script interface extends `JavaProject` it inherits a `clean()` method which can be specified as a target:

<pre>
% ./make-simple clean
<span style="color:#aa0">[execute]</span> clean
<span style="color:#aa0">[execute]</span>   deleteBuildDir ''
<span style="color:#0a0">[current]</span>     buildPath
<span style="color:#0d0">COMPLETED in 36ms</span>
</pre>

There is also a `targets()` method which prints all the targets and their return types:

<pre>
% ./make-simple targets
<span style="color:#aa0">[execute]</span> targets<b>
Project targets
  classes : Fileset
  sources : Fileset
  jarfile : File
  clean : void
  targets : void
  help : void
  buildPath : String
  sourcePath : String</b>
<span style="color:#0d0">COMPLETED in 15ms</span>
</pre>

## Building the Jam library

This repo does not use any standard build tools like Ant or Maven.
In fact, it builds itself.
To build this library and produce `jam.jar` requires two steps

1. Bootstrap the main classes by running `./setup`
2. Run either the `./make-jam` (Java) or `./make-jam.main.kts` (Kotlin) build script to compile Jam, run unit tests, and package everything in `jam.jar`





