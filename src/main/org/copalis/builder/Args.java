package org.copalis.builder;

import java.util.LinkedList;
import java.util.List;

/**
 * A utility class for constructing vararg parameter lists.
 *
 * @author giles
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
     * Returns the arguments as an array
     * @return an array containing the vararg list
     */
    public String[] array() {
        return list.toArray(String[]::new);
    }
}
