package einvoice.i18n;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Guards the invoice texts: every language is complete and the stylesheet knows every label. */
class InvoiceTextsTest {
    private static final Pattern STYLESHEET_PARAMETER =
            Pattern.compile("<xsl:param name=\"((?:label|unit)\\.[^\"]+)\"");

    @Test
    void allLanguagesDefineTheSameKeys() {
        Set<String> reference = InvoiceTexts.of(Language.GERMAN).keys("");
        assertFalse(reference.isEmpty());
        for (Language language : Language.values()) {
            assertEquals(reference, InvoiceTexts.of(language).keys(""), "keys of " + language);
        }
    }

    @Test
    void stylesheetDeclaresAParameterForEveryLabelAndUnit() throws IOException {
        Set<String> declared = new TreeSet<>();
        try (InputStream in = InvoiceTextsTest.class.getResourceAsStream("/einvoice/template.xsl")) {
            Matcher matcher = STYLESHEET_PARAMETER.matcher(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            while (matcher.find()) {
                declared.add(matcher.group(1));
            }
        }
        assertEquals(InvoiceTexts.of(Language.GERMAN).keys("label.", "unit."), declared);
    }

    @Test
    void missingLanguageMeansGerman() {
        assertEquals(Language.GERMAN, InvoiceTexts.of(null).language());
        assertEquals("Rechnung Nr.", InvoiceTexts.of(null).get("label.invoiceTitle"));
        assertEquals("Invoice No.", InvoiceTexts.of(Language.ENGLISH).get("label.invoiceTitle"));
    }
}
