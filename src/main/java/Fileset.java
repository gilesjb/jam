import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.copalis.builder.Checked;

/**
 * A reference to a set of existing files
 *
 * @author giles
 */
public final class Fileset implements Checked, Iterable<File> {

    private static final long serialVersionUID = -6221550505534926198L;

    public static final Collector<File, Object, Fileset> FILES =
            Collectors.collectingAndThen(Collectors.toSet(), Fileset::of);

    final Set<File> files;
    final String base, pattern;

    public static Fileset of(Set<File> files) {
        return new Fileset(files, null, null);
    }

    private Fileset(Set<File> files, String base, String pattern) {
        this.files = files;
        this.base = base;
        this.pattern = pattern;
    }

    public static Fileset find(String base, String pattern) {
        Path path = Path.of(base);
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
        Set<File> matches = new TreeSet<>();
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (matcher.matches(path.relativize(file))) {
                        matches.add(new File(file));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new Fileset(matches, base, pattern);
    }

    public Fileset add(File file) {
        TreeSet<File> set = new TreeSet<>(files);
        set.add(file);
        return of(set);
    }

    public Fileset union(Fileset fs) {
        TreeSet<File> set = new TreeSet<>(files);
        set.addAll(fs.files);
        return of(set);
    }

    public Fileset map(Function<File, File> fn) {
        return files.stream().map(fn).collect(FILES);
    }

    public Stream<File> stream() {
        return files.stream();
    }

    public boolean isCurrent() {
        return (Objects.isNull(pattern) || Objects.equals(this, find(base, pattern)))
                && files.stream().map(File::isCurrent).allMatch(Boolean.TRUE::equals);
    }

    @Override public Iterator<File> iterator() {
        return files.iterator();
    }

    @Override public String toString() {
        return files.stream().map(File::toString).collect(Collectors.joining(";"));
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
