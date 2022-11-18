import java.io.IOException;
import java.io.Serializable;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.copalis.builder.Checked;

public class Fileset implements Checked, Serializable {

    private static final long serialVersionUID = -6221550505534926198L;

    public static final Collector<File, Object, Fileset> COLLECT =
            Collectors.collectingAndThen(Collectors.toSet(), Fileset::new);

    private final Set<File> files;
    private final String base, pattern;

    public Fileset(Set<File> files) {
        this(files, null, null);
    }

    private Fileset(Set<File> files, String base, String pattern) {
        this.files = files;
        this.base = base;
        this.pattern = pattern;
    }

    static Fileset find(String base, String pattern) {
        Path path = Path.of(base);
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
        Set<File> matches = new LinkedHashSet<>();
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
        LinkedHashSet<File> set = new LinkedHashSet<>(files);
        set.add(file);
        return new Fileset(set);
    }

    public Fileset union(Fileset fs) {
        LinkedHashSet<File> set = new LinkedHashSet<>(files);
        set.addAll(fs.files);
        return new Fileset(set);
    }

    public Fileset map(Function<File, File> fn) {
        return new Fileset(files.stream().map(fn).collect(Collectors.toSet()));
    }

    public Stream<File> stream() {
        return files.stream();
    }

    public boolean isCurrent() {
        return (Objects.isNull(pattern) || Objects.equals(this, find(base, pattern)))
                && files.stream().map(File::isCurrent).allMatch(Boolean.TRUE::equals);
    }

    @Override public String toString() {
        return Objects.toString(files);
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
