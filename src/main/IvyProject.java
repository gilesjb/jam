import org.copalis.jam.IvyResolver;

/**
 * Project mixin which provides an implementation of {@link JavaProject#packageResolver()}
 * that uses Apache Ivy
 *
 * @author gilesjb
 */
public interface IvyProject extends JavaProject {

    /**
     * {@inheritDoc}
     * <p>
     * This implementation uses Apache Ivy
     * @return an instance of IvyResolver
     */
    @Override default IvyResolver packageResolver() {
        return new IvyResolver(IvyResolver.VER2_5_1_URL, ".package-cache");
    }
}
