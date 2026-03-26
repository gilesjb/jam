package org.copalis.jam.util;

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
 * @param settingsFile a specific file to use for Ivy settings
 *
 * @see <a href="https://ant.apache.org/ivy/history/2.5.0/standalone.html">Ivy standalone</a>
 * @author giles
 */
public record IvyResolver(String url, String cacheDir, File settingsFile) implements PackageResolver, Serializable {
    /**
     * The default URL of the Ivy runtime jar
     */
    public static final String VER2_5_1_URL = "https://repo1.maven.org/maven2/org/apache/ivy/ivy/2.5.1/ivy-2.5.1.jar";

    /**
     * Gets a reference to a local copy of the Ivy jar file
     * @return the file path
     */
    public Path ivyJar() {
        Path path = Path.of(cacheDir, url.substring(url.lastIndexOf('/') + 1));
        if (!path.toFile().exists()) {
            System.out.print("Downloading " + url + "... ");
            System.out.format("(%skB)%n", Paths.download(path, url) / 1024);
        }
        return path;
    }

    @Override public Stream<Path> resolve(String... dependencies) {
        StringBuilder xml = new StringBuilder();
        xml.append("<ivy-module version='2.0' xmlns:m='http://ant.apache.org/ivy/maven'>");
        xml.append("<info organisation='org' module='module'/>");
        xml.append("<dependencies defaultconf='*->default'>");

        for (String ident : dependencies) {
            int pound = ident.indexOf('#');
            if (pound >= 0) {
                String[] parts = ident.substring(0, pound).split(":");
                xml.append(String.format("<dependency org='%s' name='%s' rev='%s' m:classifier='%s'/>",
                        parts[0], parts[parts.length - 2], parts[parts.length - 1], ident.substring(pound + 1)));
            } else {
                String[] parts = ident.split(":");
                xml.append(String.format("<dependency org='%s' name='%s' rev='%s' />",
                        parts[0], parts[parts.length - 2], parts[parts.length - 1]));
            }
        }
        xml.append("</dependencies></ivy-module>");
        return download(tempFile("ivy-", ".xml", xml), settingsFile);
    }

    /**
     * Executes Ivy
     * @param ivyFile an optional ivy.xml file
     * @param settingsFile an optional settings.xml file
     * @return a stream of Path objects referencing the resolved dependencies
     */
    public Stream<Path> download(File ivyFile, File settingsFile) {
        File cacheFile = tempFile("classpath-", null, null);

        Args cmd = Args.of("java", "-jar", ivyJar().toString(),
                "-cache", cacheDir,
                "-cachepath", cacheFile.toString());

        if (Objects.nonNull(ivyFile)) {
            cmd.and("-ivy", ivyFile.toString());
        }

        if (Objects.nonNull(settingsFile)) {
            cmd.and("-settings", settingsFile.toString());
        }
        cmd.run();
        try {
            return Stream.of(Files.readString(cacheFile.toPath()).trim().split(":")).map(Path::of);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private File tempFile(String prefix, String suffix, CharSequence content) {
        File tmp;
        try {
            tmp = File.createTempFile(prefix, suffix);
            tmp.deleteOnExit();
            if (Objects.nonNull(content)) {
                Files.writeString(tmp.toPath(), content);
            }
            return tmp;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
