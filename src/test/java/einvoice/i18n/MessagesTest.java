package einvoice.i18n;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the translations: every language has every text, and the code only uses texts that exist. */
class MessagesTest {
    private static final Pattern KEY_USAGE = Pattern.compile("Messages\\.get\\(\"([^\"]+)\"");
    private static final Pattern KEY_CONSTANT = Pattern.compile("\\(\"((?:nav|taxCase)\\.[A-Za-z0-9.]+)\"\\)");

    @Test
    void allLanguagesDefineTheSameKeys() {
        Set<String> reference = keys(Language.GERMAN);
        for (Language language : Language.values()) {
            assertEquals(reference, keys(language), "keys of " + language);
        }
    }

    @Test
    void everyKeyUsedInTheCodeExists() throws IOException {
        Set<String> defined = keys(Language.GERMAN);
        Set<String> used = new TreeSet<>();
        try (Stream<Path> files = Files.walk(Path.of("src", "main", "java"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                collect(KEY_USAGE.matcher(source), used);
                collect(KEY_CONSTANT.matcher(source), used);
            }
        }
        assertFalse(used.isEmpty());
        used.removeAll(defined);
        assertTrue(used.isEmpty(), "keys without text: " + used);
    }

    @Test
    void textsFollowTheSelectedLanguage() {
        Language previous = Messages.language();
        try {
            Messages.setLanguage(Language.ENGLISH);
            assertEquals("Customers", Messages.get("nav.customers"));
            assertEquals("Line item 2:", Messages.get("check.item.prefix", 2));
            Messages.setLanguage(Language.GERMAN);
            assertEquals("Kunden", Messages.get("nav.customers"));
        } finally {
            Messages.setLanguage(previous);
        }
    }

    private static Set<String> keys(Language language) {
        ResourceBundle bundle = ResourceBundle.getBundle(Messages.BUNDLE, language.locale(),
                ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));
        return new TreeSet<>(bundle.keySet());
    }

    private static void collect(Matcher matcher, Set<String> keys) {
        while (matcher.find()) {
            keys.add(matcher.group(1));
        }
    }
}
