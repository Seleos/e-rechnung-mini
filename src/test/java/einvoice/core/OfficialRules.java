package einvoice.core;

import net.sf.saxon.TransformerFactoryImpl;

import javax.xml.transform.Templates;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The official EN 16931 business rules as published by the European standards committee (CEN/TC 434),
 * applied independently of the library the app uses to generate and validate invoices.
 */
final class OfficialRules {
    private static final String RULES = "/external/schematron/1.3.17/cii/EN16931-CII-validation.xslt";
    private static final Pattern FAILED_RULE =
            Pattern.compile("<svrl:failed-assert[^>]*flag=\"fatal\"[^>]*>\\s*<svrl:text>([^<]*)", Pattern.DOTALL);
    private static final Templates TEMPLATES = load();

    private OfficialRules() {
    }

    /**
     * Checks an invoice XML against the official rules.
     *
     * @param xml invoice in CII syntax
     * @return the messages of all violated rules, empty if the invoice passes
     * @throws Exception if the rules cannot be applied
     */
    static List<String> violations(byte[] xml) throws Exception {
        StringWriter report = new StringWriter();
        TEMPLATES.newTransformer().transform(new StreamSource(new ByteArrayInputStream(xml)), new StreamResult(report));
        List<String> violations = new ArrayList<>();
        Matcher matcher = FAILED_RULE.matcher(report.toString());
        while (matcher.find()) {
            violations.add(matcher.group(1).trim());
        }
        return violations;
    }

    private static Templates load() {
        try (InputStream in = OfficialRules.class.getResourceAsStream(RULES)) {
            if (in == null) {
                throw new IllegalStateException("official rule set not found on the test class path");
            }
            return new TransformerFactoryImpl().newTemplates(new StreamSource(in));
        } catch (Exception e) {
            throw new IllegalStateException("official rule set cannot be loaded", e);
        }
    }
}
