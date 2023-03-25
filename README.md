# Jam - a build tool

## How is Jam different?

Most build tools expect you to write a build file that is executed by the tool.
For example, when you run `ant` it reads a file called `build.xml` and executes the tasks defined inside.

Jam inverts this process.
A Jam build file is an executable Java or Kotlin script.
The script defines the build process in code, as a set of methods,
and calls the Jam library to manage the execution.

Jam is also different in how it expresses dependencies. 
Rather than specifying that a target `dependsOn` files produced by another target,
in Jam every target is a method that returns its output files,
so a target "depends on" another by calling it.
Jam ensures that these method calls are deduplicated by using a result cache and method interceptors.

## How it works

1. You run your build script from the command line
2. A shebang (#!) in the script instructs Java or Kotlin to execute the script, with `jam.jar` on the classpath
3. Your script defines an interface containing build logic methods
4. Your script's `main` method calls Jam's `Project.make(...)`, passing in the interface
5. Jam uses a Dynamic Proxy to instantiate the interface with method interceptors
6. Jam calls the target method, caching and deduplicating all method calls

## Using Jam from Java

To use Jam you need an installed JDK and a copy of `jam.jar`.

Create a file called `build-hello` with the following contents

```java
#!/usr/bin/java -classpath jam.jar --source 17

public interface FriendlyScript extends Project {
    default String helloText() {
        return "Hello, world!";
    }

    default void build() {
        System.out.println(helloText());
    }

    public static void main(String[] args) {
        Project.make(FriendlyScript.class, FriendlyScript::build, args);
    }
}
```

If you run `./build-hello targets` the console will show

```
[execute] targets
Project targets
  build : void
  helloText : String
  clean : void
  targets : void
  help : void
  buildPath : String
  sourcePath : String
COMPLETED in 9ms  
```

A target is just a 0-parameter method. The targets `helloText` and `build` are defined in `FriendlyScript` interface while the other targets are inherited from `Project`.

If you run `./build-hello` without a target parameter it will execute the default target specified by the script,
which was `FriendlyScript::build`.

```
[execute] build
[execute]   helloText
Hello, world!
COMPLETED in 8ms
```

This console output shows that `build()` was called, and that in turn called `helloText()`.

## Building the Jam library

This repo does not use any standard build tools like Ant or Maven.
In fact, it builds itself.
To build this library and produce `jam.jar` requires two steps

1. Bootstrap the main classes by running `./setup`
2. Run either `./make-jam` (Java) or `./make-jam.main.kts` (Kotlin) build script to compile Jam, run unit tests, and package everything in `jam.jar`





