import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface Builder {

    final Memorizer memo = new Memorizer();

    /**
     * Returns the root source directory
     * @return the path of the source directory
     */
    default String srcPath() {
        return "src";
    }

    /**
     * Returns the root build directory
     * @return the path of the build directory
     */
    default String buildPath() {
        return "build";
    }

    /**
     * Gets files matching a pattern
     * @param pattern
     * @return a fileset of the matching files
     */
    default Fileset sources(String pattern) {
        return memo.dependsOn(Path.of(srcPath(), pattern).toString(),
                Fileset.find(Path.of(srcPath()), pattern));
    }

    /**
     * Gets the file with a specific path
     * @param name
     * @return a File object referencing the specified path
     */
    default File srcFile(String name) {
        return new File(Path.of(srcPath()).resolve(name));
    }

    /**
     * Creates a file and writes content to it
     * @param name
     * @param content
     * @return a File object referencing the created file
     */
    default File write(String name, String content) {
        Path path = Path.of(buildPath()).resolve(name);
        try {
            Files.createDirectories(path.getParent());
            return new File(Files.writeString(path, content));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static <T extends Builder> void make(Class<T> t, Function<T, ?> fn, String[] args) {
        T maker = memo.instantiate(t);
        String buildPath = maker.buildPath();
        memo.loadCache(buildPath);
        memo.validateCache();
        memo.setListener(new Memorizer.Listener() {
            @Override public void completed(boolean cached, Method method, List<Object> params, Object result) {
                if (cached || !t.isAssignableFrom(method.getDeclaringClass())) {
                } else if (!params.isEmpty()) {
                    System.out.println("    " + method.getName()
                            + params.stream().map(Object::toString).collect(Collectors.joining(",", "(", ")")) + " => "
                            + result);
                } else {
                    System.out.println("    " + method.getName() + " => " + result);
                }
            }
        });
        final Object result;
        if (args.length > 0) {
            try {
                Method m = t.getMethod(args[0]);
                result = m.invoke(maker);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            result = fn.apply(maker);
        }
        System.out.println("-> " + result);
        memo.saveCache(buildPath);
    }
}
