package einvoice;

import einvoice.core.InvoiceValidator;
import einvoice.core.PdfBuilder;
import einvoice.i18n.Language;
import einvoice.model.Company;
import einvoice.model.Customer;
import einvoice.model.InvoiceData;
import einvoice.model.LineItem;
import einvoice.model.TaxCase;
import einvoice.service.InvoiceService;
import einvoice.storage.DataStore;
import einvoice.storage.SampleData;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Generates an invoice for every tax case and validates the resulting ZUGFeRD file. */
class InvoiceGenerationTest {
    private static final Path OUTPUT = Path.of("target", "test-output");


    private static DataStore store;
    private static InvoiceService service;
    private static Company company;

    @BeforeAll
    static void setUp(@TempDir Path dataDir) throws Exception {
        store = new DataStore(dataDir);
        service = new InvoiceService(store);
        company = SampleData.create(store);
        Files.createDirectories(OUTPUT);
    }

    @Test
    void domesticStandardRateIsValid() throws Exception {
        assertValid("domestic-19", company, TestData.customer(TaxCase.DOMESTIC_19, "DE", "DE111111111"));
    }

    @Test
    void domesticReducedRateIsValid() throws Exception {
        assertValid("domestic-7", company, TestData.customer(TaxCase.DOMESTIC_7, "DE", ""));
    }

    @Test
    void reverseChargeIsValid() throws Exception {
        assertValid("reverse-charge", company, TestData.customer(TaxCase.EU_REVERSE_CHARGE, "FR", "FR12345678901"));
    }

    @Test
    void thirdCountryIsValid() throws Exception {
        assertValid("third-country", company, TestData.customer(TaxCase.THIRD_COUNTRY, "US", ""));
    }

    @Test
    void smallBusinessIsValid() throws Exception {
        Company smallBusiness = store.companies().get(0);
        smallBusiness.setSmallBusiness(true);
        smallBusiness.setVatId("");
        assertValid("small-business", smallBusiness, TestData.customer(TaxCase.DOMESTIC_19, "DE", ""));
    }

    @Test
    void englishInvoiceIsValid() throws Exception {
        Customer customer = TestData.customer(TaxCase.EU_REVERSE_CHARGE, "FR", "FR12345678901");
        customer.setInvoiceLanguage(Language.ENGLISH);
        assertValid("english-reverse-charge", company, customer);
    }

    @Test
    void foreignCurrencyInvoiceIsValid() throws Exception {
        Customer customer = TestData.customer(TaxCase.THIRD_COUNTRY, "US", "");
        customer.setInvoiceLanguage(Language.ENGLISH);
        customer.setCurrency("USD");
        assertValid("english-usd", company, customer);
    }

    @Test
    void specialCharactersSurviveInXmlAndPdf() throws Exception {
        Customer customer = TestData.domesticCustomer();
        customer.setName("Müller & Söhne <Bürobedarf> \"Größe\" GmbH");
        customer.setStreet("Rue de l'Église 5 / Straße 7");
        InvoiceData invoice = TestData.invoice(customer);
        invoice.getItems().get(0).setText("Beratung & Planung <Phase 1>\nÄnderung – 50 % Rabatt, Preis 100 € ≈ fair");

        byte[] pdf = service.build(company, invoice);
        Files.write(OUTPUT.resolve("special-characters.pdf"), pdf);

        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = textOfPage(document, 1);
            assertTrue(text.contains("Müller & Söhne <Bürobedarf> \"Größe\" GmbH"), text);
            assertTrue(text.contains("Rue de l'Église 5 / Straße 7"), text);
            assertTrue(text.contains("Beratung & Planung <Phase 1>"), text);
            assertTrue(text.contains("Änderung – 50 % Rabatt, Preis 100 €"), text);
        }
        InvoiceValidator.Result result = InvoiceValidator.validate(pdf, "special-characters.pdf");
        assertTrue(result.valid(), () -> "invoice with special characters is invalid: " + result.problems());
    }

    @Test
    void longInvoiceSpansSeveralPagesAndStaysValid() throws Exception {
        InvoiceData invoice = TestData.invoice(TestData.domesticCustomer());
        invoice.getItems().clear();
        for (int position = 1; position <= 60; position++) {
            invoice.getItems().add(new LineItem(new BigDecimal("1.5"), "h",
                    "Leistung " + position + "\nBeschreibung zur Leistung " + position, new BigDecimal("80.00")));
        }

        byte[] pdf = service.build(company, invoice);
        Files.write(OUTPUT.resolve("multi-page.pdf"), pdf);

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertTrue(document.getNumberOfPages() >= 3, "pages: " + document.getNumberOfPages());
            String lastPage = textOfPage(document, document.getNumberOfPages());
            String secondPage = textOfPage(document, 2);
            ImageIO.write(new PDFRenderer(document).renderImageWithDPI(1, 80), "png",
                    OUTPUT.resolve("multi-page-2.png").toFile());

            assertTrue(secondPage.contains("Rechnung Nr. RE-2026-00043"), "following pages repeat the number");
            assertTrue(secondPage.contains("Seite 2"), "following pages are numbered");
            assertTrue(secondPage.contains("Einzelpreis"), "the table header is repeated");
            assertTrue(secondPage.contains("Musterbank"), "the footer is on every page");
            assertTrue(lastPage.contains("Leistung 60"), "the last line is printed");
            assertTrue(lastPage.contains("8.568,00"), "the gross total of 60 x 1.5 h x 80 euro plus VAT is printed");
        }
        InvoiceValidator.Result result = InvoiceValidator.validate(pdf, "multi-page.pdf");
        assertTrue(result.valid(), () -> "multi-page invoice is invalid: " + result.problems());
    }

    private static String textOfPage(PDDocument document, int page) throws java.io.IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(page);
        stripper.setEndPage(page);
        return stripper.getText(document);
    }

    @Test
    void createSavesFileAndRecordAndNeverOverwrites() throws Exception {
        InvoiceData invoice = TestData.invoice(TestData.customer(TaxCase.DOMESTIC_19, "DE", ""));
        invoice.setNumber(store.nextNumber(company, invoice.getDate()));
        assertEquals("RE-2026-00001", invoice.getNumber());

        Path file = service.create(company, invoice);

        assertTrue(Files.size(file) > 0);
        assertEquals(new BigDecimal("562.28"), invoice.getGross());
        assertEquals(new BigDecimal("472.50"), store.records(company).get(0).getNet());
        assertEquals(new BigDecimal("89.78"), store.records(company).get(0).getTax());
        assertEquals(1, store.records(company).size());
        assertEquals("RE-2026-00002", store.nextNumber(company, invoice.getDate()));
        assertEquals("RE-2026-00001.pdf", store.records(company).get(0).getFile(), "only the file name is stored");
        assertEquals(file, service.file(company, store.records(company).get(0)));
        assertThrows(java.nio.file.FileAlreadyExistsException.class, () -> service.create(company, invoice));

        // Even if the PDF was moved away, the number stays taken because its record exists.
        Files.delete(file);
        assertThrows(java.nio.file.FileAlreadyExistsException.class, () -> service.create(company, invoice));
        assertEquals(1, store.records(company).size());
    }

    @Test
    void documentInformationNamesTheAppAndMatchesTheXmpMetadata() throws Exception {
        byte[] pdf = service.build(company, TestData.invoice(TestData.domesticCustomer()));

        try (PDDocument document = Loader.loadPDF(pdf)) {
            String producer = AppInfo.name() + " " + AppInfo.version();
            assertEquals(producer, document.getDocumentInformation().getProducer());
            assertEquals(AppInfo.name(), document.getDocumentInformation().getCreator());
            assertEquals(company.getName(), document.getDocumentInformation().getAuthor());
            String xmp = new String(document.getDocumentCatalog().getMetadata().toByteArray(), StandardCharsets.UTF_8);
            assertTrue(xmp.contains("<pdf:Producer>" + producer + "</pdf:Producer>"), xmp);
            assertFalse(xmp.contains("${"), xmp);
        }
    }

    private static void assertValid(String name, Company issuer, Customer customer) throws Exception {
        InvoiceData invoice = TestData.invoice(customer);
        byte[] pdf = service.build(issuer, invoice);
        Files.write(OUTPUT.resolve(name + ".pdf"), pdf);
        ImageIO.write(PdfBuilder.renderFirstPage(pdf, 80), "png", OUTPUT.resolve(name + ".png").toFile());

        InvoiceValidator.Result result = InvoiceValidator.validate(pdf, name + ".pdf");
        Files.writeString(OUTPUT.resolve(name + "-report.xml"), result.report());
        assertTrue(result.valid(), () -> name + " is invalid: " + result.problems());
    }
}
