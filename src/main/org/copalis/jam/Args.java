package org.copalis.jam;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.LinkedList;
import java.util.List;

/**
 * A utility class for constructing vararg parameter lists.
 *
 * @author gilesjb
 */
public class Args {
    private final List<String> list = new LinkedList<>();

    private Args() { }

    /**
     * Creates a new list with the supplied arguments
     * @param args an arbitrary number of arguments
     * @return the new list
     */
    public static Args of(String... args) {
        return new Args().add(args);
    }

    /**
     * Adds supplied arguments and returns the current list
     * @param args an arbitrary number of arguments
     * @return the current list
     */
    public Args add(String... args) {
        for (String arg : args) {
            list.add(arg);
        }
        return this;
    }

    /**
     * Adds another args list to this
     * @param other another args
     * @return the current list
     */
    public Args add(Args other) {
        list.addAll(other.list);
        return this;
    }

    /**
     * Returns the arguments as an array
     * @return an array containing the vararg list
     */
    public String[] array() {
        return list.toArray(String[]::new);
    }

    /**
     * Executes an external process using these arguments
     */
    public void exec() {
        ProcessBuilder pb = new ProcessBuilder();
        pb.command(array());
        pb.redirectError(Redirect.INHERIT);
        pb.redirectOutput(Redirect.INHERIT);
        try {
            Process proc = pb.start();
            int status = proc.waitFor();
            if (status != 0) {
                throw new RuntimeException("Process exited with status code: " + status);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
