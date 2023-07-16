package org.copalis.jam;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A wrapper for the Apache Ivy dependency manager
 *
 * @param url the URL of the Ivy jar file
 * @param cacheDir the path where the Ivy cache directory should be created
 * @param ivyFile the ivy.xml file for the project
 *
 * @see <a href="https://ant.apache.org/ivy/history/2.5.0/standalone.html">Ivy standalone</a>
 * @author giles
 */
public record IvyResolver(String url, String cacheDir, File ivyFile) implements Serializable {
    public static final String VER2_5_1 = "https://repo1.maven.org/maven2/org/apache/ivy/ivy/2.5.1/ivy-2.5.1.jar";

    /**
     * Create a resolver
     * @param cacheDir the path where the Ivy cache directory should be created
     * @param ivyFile the ivy.xml file for the project
     */
    public IvyResolver(String cacheDir, File ivyFile) {
        this(VER2_5_1, cacheDir, ivyFile);
    }

    private Path ivyJar() {
        Path path = Path.of(cacheDir, url.substring(url.lastIndexOf('/') + 1));
        if (!path.toFile().exists()) {
            System.out.print("Downloading " + url + "... ");
            Paths.download(path, url);
            System.out.println("Done");
        }
        return path;
    }

    private Stream<Path> resolve(Cmd args) {
        try {
            java.io.File pathFile = File.createTempFile("classpath-", null);
            Cmd.args("java", "-jar", ivyJar().toString(), "-cache", cacheDir,
                    "-cachepath", pathFile.toString())
                    .add(args)
                    .run();

            pathFile.deleteOnExit();
            String path = Files.readString(pathFile.toPath());
            System.out.println("Path: " + path);
            return Stream.of(path.trim().split(":"))
                    .map(Path::of);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Resolves named configurations defined in the Ivy XML file
     * @param confs the configuration names
     * @return a stream of paths of the resolved dependency
     */
    public Stream<Path> resolveConfigurations(String... confs) {
        if (Objects.isNull(ivyFile)) {
            throw new RuntimeException("No ivy.xml file has been specified");
        }
        Cmd args = Cmd.args("-ivy", ivyFile.toString());
        if (confs.length > 0) {
            args.add("-confs").add(confs);
        }
        return resolve(args);
    }

    /**
     * Resolves a named dependency and configurations
     * @param org org name
     * @param name dependency name
     * @param version version id
     * @param confs configuration names
     * @return a stream of paths of the resolved dependency
     */
    public Stream<Path> resolveNamedDependency(String org, String name, String version, String... confs) {
        Cmd args = Cmd.args("-dependency", org, name, version);
        if (confs.length > 0) {
            args.add("-confs").add(confs);
        }
        return resolve(args);
    }
}
