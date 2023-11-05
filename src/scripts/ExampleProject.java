/* Delete this line when using as command line script
#!/usr/bin/java -classpath jam-classes --source 19
/*
 */

public interface ExampleProject extends JavaProject {
    default Fileset sources() {
        return sourceFiles("main/**.java");
    }

    default Fileset classes() {
        return javac("classes/main", sources());
    }

    default File jarfile() {
        return jar("example.jar", classes());
    }

    static void main(String[] args) {
        Project.run(ExampleProject.class, ExampleProject::jarfile, args);
    }
}
