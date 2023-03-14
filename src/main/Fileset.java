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

    public static final Collector<File, Object, Fileset> FILES =
            Collectors.collectingAndThen(Collectors.toSet(), Fileset::of);

    final Set<File> files;
    final String base, pattern;

    public static Fileset of(File... files) {
        return new Fileset(new TreeSet<>(Arrays.asList(files)), null, null);
    }

    public static Fileset of(Set<File> files) {
        return new Fileset(files, null, null);
    }

    public Fileset(Set<File> files, String base, String pattern) {
        this.files = files;
        this.base = base;
        this.pattern = pattern;
    }

    public static Fileset find(String selector) {
        int split = selector.lastIndexOf('/');
        return find(selector.substring(0, split), selector.substring(split + 1));
    }

    public static Fileset find(String base, String pattern) {
        Path path = Path.of(base);
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

    public Fileset map(Function<File, File> fn) {
        return stream().map(fn).collect(FILES);
    }

    public Stream<File> stream() {
        return files.stream();
    }

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
