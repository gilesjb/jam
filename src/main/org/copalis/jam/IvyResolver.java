package org.copalis.jam;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A wrapper for the Apache Ivy dependency manager
 *
 * @param url the URL of the Ivy jar file
 * @param cacheDir the path where the Ivy cache directory should be created
 *
 * @see <a href="https://ant.apache.org/ivy/history/2.5.0/standalone.html">Ivy standalone</a>
 * @author giles
 */
public record IvyResolver(String url, String cacheDir) implements PackageResolver {
    /**
     * The default URL of the Ivy runtime jar
     */
    public static final String VER2_5_1_URL = "https://repo1.maven.org/maven2/org/apache/ivy/ivy/2.5.1/ivy-2.5.1.jar";

    private Path ivyJar() {
        Path path = Path.of(cacheDir, url.substring(url.lastIndexOf('/') + 1));
        if (!path.toFile().exists()) {
            System.out.print("Downloading " + url + "... ");
            System.out.format("(%skB)%n", Paths.download(path, url) / 1024);
        }
        return path;
    }

    /**
     * Invokes the Ivy runtime
     * @param cacheFile the file which will contain the paths of all resolved dependencies
     * @param ivyFile the ixy.xml file
     */
    private void execute(File cacheFile, File ivyFile) {
        Cmd args = Cmd.args("java", "-jar", ivyJar().toString(), "-cache", cacheDir);

        if (Objects.nonNull(cacheFile)) {
            args.add("-cachepath", cacheFile.toString());
        }

        if (Objects.nonNull(ivyFile)) {
            args.add("-ivy", ivyFile.toString());
        }

        args.run();
    }

    private void writeXML(Writer out, String... dependencies) throws IOException {
        out.write("<ivy-module version='2.0'>");
        out.write("<info organisation='org' module='module'/>");
        out.write("<dependencies defaultconf='*->default'>");
        for (String ident : dependencies) {
            String[] parts = ident.split(":");
            out.write(String.format("<dependency org='%s' name='%s' rev='%s'/>",
                    parts[0], parts[parts.length - 2], parts[parts.length - 1]));
        }
        out.write("</dependencies></ivy-module>");
    }

    @Override public Stream<Path> resolve(String... dependencies) {
        try {
            File ivyFile = File.createTempFile("ivy-", ".xml");
            ivyFile.deleteOnExit();

            try (Writer out = new FileWriter(ivyFile)) {
                writeXML(out, dependencies);
            }

            File cacheFile = File.createTempFile("classpath-", null);
            cacheFile.deleteOnExit();

            execute(cacheFile, ivyFile);
            return Stream.of(Files.readString(cacheFile.toPath()).trim().split(":"))
                    .map(Path::of);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override public void cleanCache() {
        Paths.rmDir(Path.of(cacheDir));
    }
}
