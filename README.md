# Jam - a functional build tool

## Using Jam from Java

To use Jam you need an installed JDK and a copy of `jam.jar`.

Create a file called `build-hello` with the following contents

```java
#!/usr/bin/java -classpath jam.jar --source 17

public interface FriendlyScript extends JavaProject {
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

If you run `./build-hello` the console will show

```
[execute] build
[execute]   helloText
Hello, world!
```

## Building the Jam library

This repo does not use any standard build tools like Ant or Maven.
In fact, it builds itself.
To build this library and produce `jam.jar` requires two steps

1. Bootstrap the main classes by running `./setup`
2. Run either `./make-jam` (Java) or `./make-jam.main.kts` (Kotlin) build script to compile Jam, run unit tests, and package everything in `jam.jar`

