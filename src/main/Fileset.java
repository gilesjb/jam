import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.copalis.builder.Memorizable;

/**
 * A reference to a set of existing files
 *
 * @author giles
 */
public final class Fileset implements Memorizable, Iterable<File> {

    private static final long serialVersionUID = -6221550505534926198L;

    /**
     * A Collector that collects a stream of File objects into a Fileset
     */
    public static final Collector<File, Object, Fileset> FILES =
            Collectors.collectingAndThen(Collectors.toSet(), Fileset::of);

    /**
     * The files that belong to this set
     */
    final Set<File> files;
    /**
     * The base directory that all the files are under
     */
    final String base;
    /**
     * The glob pattern that all the files match
     */
    final String pattern;

    /**
     * Creates a Fileset from an array of File objects
     * @param files the File objects
     * @return a new Fileset
     */
    public static Fileset of(File... files) {
        return new Fileset(new TreeSet<>(Arrays.asList(files)), null, null);
    }

    /**
     * Creates a Fileset from a set of File objects
     * @param files the File objects
     * @return a new Fileset
     */
    public static Fileset of(Set<File> files) {
        return new Fileset(files, null, null);
    }

    /**
     * Constructs a Fileset
     * @param files the set of File objects
     * @param base the base path of the files, or null
     * @param pattern the glob pattern for the files, or null
     */
    public Fileset(Set<File> files, String base, String pattern) {
        this.files = files;
        this.base = base;
        this.pattern = pattern;
    }

    /**
     * Creates a Fileset containing all files that match a selection string
     * @param selector the selector
     * @return a Fileset with corresponding files, base and pattern
     */
    public static Fileset find(String selector) {
        int split = selector.lastIndexOf('/');
        return find(selector.substring(0, split), selector.substring(split + 1));
    }

    /**
     * Creates a Fileset containing all files that match a selection string
     * @param base the base directory to find the files in
     * @param pattern the selection glob pattern
     * @return a Fileset with the found files and supplied base path and pattern
     */
    public static Fileset find(String base, String pattern) {
        Path path = Path.of(base);
        if (!new File(path).exists()) {
            return null;
        }
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
        try {
            TreeSet<File> matches = Files.walk(path)
                .filter(p -> matcher.matches(path.relativize(p)))
                .map(File::new)
                .collect(Collectors.toCollection(TreeSet::new));
            return new Fileset(matches, base, pattern);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a Fileset consisting of a mapping of the files in this set
     * @param fn the mapping function
     * @return the Filest of mapped files
     */
    public Fileset map(Function<File, File> fn) {
        return stream().map(fn).collect(FILES);
    }

    /**
     * Streams the files in this set
     * @return the Stream of these files
     */
    public Stream<File> stream() {
        return files.stream();
    }

    /**
     * Gets the base path of the files in this set, if there is one
     * @return The base path of this set, or null
     */
    public String base() {
        return base;
    }

    public boolean current() {
        return (Objects.isNull(pattern) || Objects.equals(this, find(base, pattern)))
                && files.stream().map(File::current).allMatch(Boolean.TRUE::equals);
    }

    @Override public Iterator<File> iterator() {
        return files.iterator();
    }

    @Override public String toString() {
        if (Objects.nonNull(pattern)) {
            return base + '/' + pattern;
        }
        return files.stream().map(File::toString).collect(Collectors.joining(":"));
    }

    @Override public int hashCode() {
        return Objects.hashCode(pattern) + files.size();
    }

    @Override public boolean equals(Object obj) {
        return !(obj instanceof Fileset other) || Objects.equals(files, other.files)
                && Objects.equals(base, other.base)
                && Objects.equals(pattern, other.pattern);
    }
}
