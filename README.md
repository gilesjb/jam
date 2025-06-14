# Jam - Just A Memoizer & build tool

A lightweight library that lets you write command-line build scripts in plain Java or Kotlin code.
Jam provides command-line option handling, logging, and dependency tracking.


## What is Jam?

Jam is a Java library that allows you to write build scrips in plain Java or Kotlin code.

### Example build scripts

Below are equivalent Kotlin and Java versions of a build script.

<details open>
<summary>Kotlin</summary>

File name: `basic.kts`

File contents:

```kotlin
#!/usr/bin/env kotlin -Xjvm-default=all -cp jam.jar

interface ExampleProject : JavaProject {

    fun mainSources() = sourceFiles("main/**.java")

    fun mainClasses() = javac("classes/main", mainSources())

    fun docs() = javadoc("docs", "-sourcepath", classpath(mainSources()), "-subpackages", "", "-quiet")

    fun all() {
        docs()
        mainClasses()
    }
}

Project.run(ExampleProject::class.java, ExampleProject::all, args)
```

The script consists of:

1. A [shebang](https://en.wikipedia.org/wiki/Shebang_(Unix)) first line which invokes the Kotlin runtime to execute this script and specifies the location of `jam.jar`.
2. A *project interface*. This defines the build targets and build logic.
3. A final call to `Project.run(ExampleProject::class.java, ExampleProject::all, args)` which executes the build controller with the project interface, the default build target, and command-line arguments.
 
</details>
<details>
<summary>Java</summary>

File name: `basic-java`

File contents:

```java
#!/usr/bin/java -classpath jam.jar --source 17

public interface ExampleProject extends JavaProject {

    default Fileset mainSources() {
        return sourceFiles("main/**.java");
    }

    default Fileset mainClasses() {
        return javac("classes/main", mainSources());
    }

    default Fileset docs() {
        return javadoc("docs",
                "-sourcepath", classpath(mainSources()),
                "-subpackages", "", "-quiet");
    }

    default void all() {
        docs();
        mainClasses();
    }

    static void main(String[] args) {
        Project.run(ExampleProject.class, ExampleProject::all, args);
    }
}
```
Boilerplate for this script consists of:

1. The [shebang](https://en.wikipedia.org/wiki/Shebang_(Unix)) first line which invokes the Java runtime to execute this script and specifies the location of `jam.jar`.
2. A *project interface*. This defines the build targets and build logic.
3. A `main()` method which invokes the build controller by calling `Project.run(ExampleProject.class, ExampleProject::all, args)` with the project interface, default build target and command-line arguments.
 
</details>

Kotlin and Java versions of the script define the same build targets, which can by viewed by running the script with the `--targets` option. 

![Title](examples/media/basic01.png)

The `all`, `mainSources`, `mainClasses` and `docs` targets correspond to the 0-argument methods defined in the `ExampleProject` interface.

Additional build targets are inherited from parent interface `JavaProject` and its parent interface `BuilderProject`, including the `clean` target which deletes all build artifacts.

Ok, let's run this build:
 
![Title](examples/media/basic02.png)

When a build script is run without any arguments, the default target is executed, which in this case is `all`.

Jam displays all the function calls that occur within the project. The first few lines show that the target function `all()` calls `docs()` which calls `mainSources()` which calls `sourceFiles("main/**.java")`, etc.

> Jam *memoizes* function calls.
> This means that when a function is executed (indicated by the `[compute]` tag,) Jam stores the result value in its cache.
> If an identical function call is made later, Jam intercepts the function call and returns the `[current]` cached value instead.

The memoization cache is persisted between runs of the build script. If we look at the build targets again:

![Title](examples/media/basic03.png)

The targets that were previously executed and had their results cached now have a `[fresh]` tag.

> Only non-void methods are cached

Let's run the build again:

![Title](examples/media/basic04.png)

The `all()` function has no return value and so can't be cached but the `docs()` and `mainClasses()` are both cacheable, and since their dependencies haven't changed their output artifacts do not need to be rebuilt.

## Dependency tracking

Jam tracks the modification time of source files that are inputs or dependencies of the functions that it memoizes.

Let's use `touch` to mark a source file as updated, and then check the status of the build targets:

![Title](examples/media/basic05.png)

Three of the build targets are now marked as `[stale]`. 

> How it works:
One of the files referenced by the `Fileset` returned by `mainSources()` has been updated, which invalidates that cached result.
Because `mainSources()` was called by both `docs()` and `mainClasses()` Jam knows they have a dependency on that result, so their results are also stale.

Now if we run the build again, Jam will rebuild aka `[refresh]` the stale targets:
 
![Title](examples/media/basic06.png)

## More advanced build scripts

Take a look at Jam's own [build script](examples/make-jam.kts). This demonstrates how to:

* Use `resolve()` to download libraries from the Maven repository and use them as build dependencies
* Run unit tests with the `junit()` method
* Build jar files

## Tips

You may have noticed that the example build scripts have no `import` statements.
Types provided by Jam are declared in the default package so for common cases you don't need to clutter your build script with imports.

Source file paths are relative to the base directory `./src`,
and build functions like `javac()` accept paths that are relatve to the build artifacts base directory, which is `./build` by default.
To change these, override the `sourcePath()` and `buildPath()` methods.

The Jam cache file is stored in the current directory, and is given the name `.{project-interface-name}.ser`

Jam follows these conventions:

* `void` methods are not memoized, and should be used for logic that has side effects
* Non-void methods with 1 or more parameters are memoized, but cannot be specified as targets

Due to how Jam's memoizer is implemented, a project must follow these rules:

* The project definition must be an interface rather than a class
* The functions must be implemented as `default` methods
* In order to be saved in the result cache, parameters and return types must be primitive or serializable

## Building the Jam library

Building `jam.jar` requires JDK 17 or higher to be installed.

1. Compile the main classes by running `./setup`
2. Run `./make-jam` to compile Jam, run unit tests, and create JavaDocs and `jam-<version>.jar`
3. Alternatively, run the equivalent Kotlin build script: `./examples/make-jam.kts`

