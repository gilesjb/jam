import org.copalis.jam.IvyResolver;

/**
 * Project mixin which provides an implementation of {@link BuilderProject#packageResolver()}
 * that uses Apache Ivy
 *
 * @author gilesjb
 */
public interface IvyProject extends BuilderProject {

    /**
     * {@inheritDoc}
     * <p>
     * This implementation uses Apache Ivy,
     * with the directory {@code .ivy2} as its local package cache
     * @return an instance of IvyResolver
     */
    @Override default IvyResolver packageResolver() {
        return new IvyResolver(IvyResolver.VER2_5_1_URL, ".ivy2");
    }
}
