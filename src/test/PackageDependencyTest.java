import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.WordUtils;
import org.junit.jupiter.api.Test;

/**
 * Tests project dependency resolution
 *
 * @author giles
 */
public class PackageDependencyTest {

    @Test public void testCommonsLang() {
        assertEquals("Hello World", WordUtils.capitalizeFully("hello world"));
    }

    @Test public void testCommonsCli() throws ParseException {
        Options options = new Options();
        options.addOption(Option.builder("m").longOpt("message").hasArg().build());

        CommandLine line = new DefaultParser().parse(options, new String[] {"-m", "hello"});

        String message = line.getOptionValue("m", "");
        assertEquals("hello", message);
    }
}
