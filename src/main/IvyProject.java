import java.nio.file.Path;

import org.copalis.jam.IvyResolver;

/**
 * Project mixin which provides an implementation of {@link Project#packageResolver()}
 * that uses Apache Ivy
 *
 * @author gilesjb
 */
public interface IvyProject extends Project {

    /**
     * {@inheritDoc}
     * <p>
     * This implementation uses Apache Ivy,
     * with the directory {@code <home-dir>/.ivy2} as its local package cache
     * @return an instance of IvyResolver
     */
    @Override default IvyResolver packageResolver() {
        return new IvyResolver(
                IvyResolver.VER2_5_1_URL,
                Path.of(System.getProperty("user.home"), ".ivy2").toString());
    }
}
