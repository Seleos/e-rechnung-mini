package einvoice.core;

import einvoice.TestData;
import einvoice.i18n.Language;
import einvoice.model.Company;
import einvoice.model.Customer;
import einvoice.model.InvoiceData;
import einvoice.model.TaxCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Compares the rendered first page of an invoice with a stored reference image. This catches unintended
 * layout changes, for example after an update of the PDF library or a change to the stylesheet.
 *
 * <p>After an intended layout change, look at the new images in {@code target/test-output} and accept
 * them with {@code ./mvnw test -Dtest=InvoiceImageTest -Deinvoice.updateReferences=true}.
 */
class InvoiceImageTest {
    private static final Path REFERENCES = Path.of("src", "test", "resources", "einvoice", "reference");
    private static final Path OUTPUT = Path.of("target", "test-output");
    private static final boolean UPDATE = Boolean.getBoolean("einvoice.updateReferences");
    private static final float DPI = 72;
    /** Difference of grey values above which a pixel counts as changed; hides anti-aliasing noise. */
    private static final int PIXEL_TOLERANCE = 64;
    /** Share of changed pixels that is still accepted, to allow for small rendering differences. */
    private static final double ALLOWED_SHARE = 0.002;

    @TempDir
    private Path logoDir;

    @Test
    void germanInvoiceLooksLikeTheReference() throws Exception {
        assertMatchesReference("invoice-de", TestData.domesticCustomer());
    }

    @Test
    void englishForeignCurrencyInvoiceLooksLikeTheReference() throws Exception {
        Customer customer = TestData.customer(TaxCase.THIRD_COUNTRY, "US", "");
        customer.setInvoiceLanguage(Language.ENGLISH);
        customer.setCurrency("USD");
        assertMatchesReference("invoice-en-usd", customer);
    }

    @Test
    void svgLogoWithoutSizeIsDrawnAtTheSizeOfItsViewBox() throws Exception {
        String sample = Files.readString(sampleLogo());
        Path sized = Files.writeString(logoDir.resolve("sized.svg"),
                sample.replaceFirst(" width=\"181\" height=\"60\"", " width=\"181\" height=\"48\""));
        Path unsized = Files.writeString(logoDir.resolve("unsized.svg"),
                sample.replaceFirst(" width=\"181\" height=\"60\"", ""));

        BufferedImage expected = render(TestData.domesticCustomer(), sized);
        BufferedImage actual = render(TestData.domesticCustomer(), unsized);

        assertEquals(0.0, changedShare(expected, actual));
    }

    private void assertMatchesReference(String name, Customer customer) throws Exception {
        BufferedImage actual = render(customer, sampleLogo());

        Path reference = REFERENCES.resolve(name + ".png");
        if (UPDATE || !Files.exists(reference)) {
            Files.createDirectories(REFERENCES);
            ImageIO.write(actual, "png", reference.toFile());
            assertTrue(UPDATE,
                    "Reference image " + reference + " did not exist and was created. Check it and run again.");
            return;
        }
        BufferedImage expected = ImageIO.read(reference.toFile());
        assertEquals(expected.getWidth() + "x" + expected.getHeight(), actual.getWidth() + "x" + actual.getHeight(),
                "page size of " + name);

        double changedShare = changedShare(expected, actual);
        if (changedShare > ALLOWED_SHARE) {
            Files.createDirectories(OUTPUT);
            Path actualFile = OUTPUT.resolve(name + "-actual.png");
            ImageIO.write(actual, "png", actualFile.toFile());
            fail(String.format("%s differs from its reference in %.2f %% of the pixels. Compare %s with %s.",
                    name, changedShare * 100, actualFile, reference));
        }
    }

    private static BufferedImage render(Customer customer, Path logo) throws Exception {
        Company company = TestData.company();
        company.setManagingDirector("Erika Mustermann");
        company.setRegisterCourt("Amtsgericht Musterstadt");
        company.setWeb("www.musterfirma.example");
        InvoiceData invoice = TestData.invoice(customer);
        byte[] xml = XmlBuilder.xml(company, invoice);
        return PdfBuilder.renderFirstPage(PdfBuilder.renderLayout(company, invoice, xml, logo, null), DPI);
    }

    private static double changedShare(BufferedImage expected, BufferedImage actual) {
        long changed = 0;
        for (int y = 0; y < expected.getHeight(); y++) {
            for (int x = 0; x < expected.getWidth(); x++) {
                if (Math.abs(grey(expected.getRGB(x, y)) - grey(actual.getRGB(x, y))) > PIXEL_TOLERANCE) {
                    changed++;
                }
            }
        }
        return (double) changed / ((long) expected.getWidth() * expected.getHeight());
    }

    private static int grey(int rgb) {
        return (((rgb >> 16) & 0xFF) + ((rgb >> 8) & 0xFF) + (rgb & 0xFF)) / 3;
    }

    private Path sampleLogo() throws IOException {
        Path logo = logoDir.resolve("logo.svg");
        if (!Files.exists(logo)) {
            try (InputStream in = InvoiceImageTest.class.getResourceAsStream("/einvoice/sample-logo.svg")) {
                Files.copy(in, logo);
            }
        }
        return logo;
    }
}
