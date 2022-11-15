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

import org.copalis.builder.Timestamped;

public class Fileset implements Timestamped, Serializable {

    private static final long serialVersionUID = -6221550505534926198L;

    public static final Collector<File, Object, Fileset> COLLECT =
            Collectors.collectingAndThen(Collectors.toSet(), Fileset::new);

    private final Set<File> files;

    public Fileset(Set<File> files) {
        this.files = files;
    }

    static Fileset find(Path base, String pattern) {
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
        Set<File> matches = new LinkedHashSet<>();
        try {
            Files.walkFileTree(base, new SimpleFileVisitor<Path>() {
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (matcher.matches(base.relativize(file))) {
                        matches.add(new File(file));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new Fileset(matches);
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
        return files.stream().map(File::isCurrent).allMatch(Boolean.TRUE::equals);
    }

    @Override public String toString() {
        return Objects.toString(files);
    }

    @Override public boolean equals(Object obj) {
        return obj instanceof Fileset && files.equals(((Fileset) obj).files);
    }
}
