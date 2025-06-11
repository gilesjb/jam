import org.copalis.jam.util.IvyResolver;

/**
 * Project mixin providing an implementation of {@link JavaProject#packageResolver()}
 * which uses Apache Ivy to download packages from the Maven repository.
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
