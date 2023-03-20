package org.copalis.builder;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * A PrintWriter that unconditionally prefixes any new line of output with a
 * given prefix.
 * <p>
 * The prefix may be a constant string, or computed on each invocation if
 * a @link PrefixProvider PrefixProvider} object is given at construction
 *
 * @version 1.1
 * @author C. Sadun
 *
 */
public class PrefixPrintWriter extends PrintWriter {

    private static char[] ls = System.getProperty("line.separator").toCharArray();
    private int truncatedNL;
    private boolean start = true;
    private boolean autoFlush = true;

    private final String prefix;

    /**
     * Build a PrefixPrintWriter which wraps the given writer, and use the
     * given @link PrefixProvider PrefixProvider} to determine the prefix.
     *
     * @param w the writer to wrap on
     * @param prefix the prefix to add to each line
     */
    public PrefixPrintWriter(Writer w, String prefix) {
        super(w, true);
        this.truncatedNL = 0;
        this.prefix = prefix;
    }

    public void write(int c) {
        write(new char[] { (char) c }, 0, 1);
    }

    public void write(String s, int off, int len) {
        write(s.toCharArray(), off, len);
    }

    public void write(char buf[], int off, int len) {
        synchronized (out) {

            if (start) {
                super.write(prefix, 0, prefix.length());
                start = false;
            }

            List<Integer> pos = new ArrayList<>();
            int truncated = truncatedNL; // Remember if we start in a truncated-newline situation
            for (int i = off; i < off + len; i++) {
                if (isNL(buf, i, off + len))
                    pos.add(i);
            }
            int p1 = 0;
            for (int p2 : pos) {
                super.write(buf, p1, p2 - p1);
                super.write(ls, 0, ls.length);
                super.write(prefix, 0, prefix.length());
                p1 = p2 + ls.length - truncated;
                if (truncated != 0)
                    truncated = 0; // Just the first time
            }
            super.write(buf, p1, off + len - p1);
            if (autoFlush)
                super.flush();
        }
    }

    /*
     * Checks if buf matches the line separator this.ls, setting this.truncatedNL if
     * a partial match exists but the buffer portion is too short for a complete
     * match
     */
    private boolean isNL(char[] buf, int start, int end) {
        for (int i = truncatedNL; i < ls.length && i < end; i++) {
            if (buf[start + i - truncatedNL] != ls[i]) {
                if (truncatedNL != 0)
                    truncatedNL = 0;
                return false;
            }
        }
        if (end - start + truncatedNL < ls.length) {
            truncatedNL = end - start;
            return false;
        }
        if (truncatedNL != 0)
            truncatedNL = 0;
        return true;
    }

    /*
     * Terminate the current line by writing the line separator string. The line
     * separator string is defined by the system property
     * <code>line.separator</code>, and is not necessarily a single newline
     * character (<code>'\n'</code>).
     */
    public void println() {
        super.println();
        start = true;
    }
}
