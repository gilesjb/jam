package org.copalis.jam.util;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.LinkedList;
import java.util.List;

/**
 * A utility class for constructing vararg parameter lists.
 *
 * @author gilesjb
 */
public class Cmd {
    private final List<String> list = new LinkedList<>();

    private Cmd() { }

    /**
     * Creates a new list with the supplied arguments
     * @param args an arbitrary number of arguments
     * @return the new list
     */
    public static Cmd args(String... args) {
        return new Cmd().add(args);
    }

    /**
     * Adds supplied arguments and returns the current list
     * @param args an arbitrary number of arguments
     * @return the current list
     */
    public Cmd add(String... args) {
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
    public Cmd add(Cmd other) {
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
    public void run() {
        ProcessBuilder pb = new ProcessBuilder();
        pb.command(array());
        pb.redirectInput(Redirect.INHERIT);
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

    @Override public String toString() {
        return list.toString();
    }
}
