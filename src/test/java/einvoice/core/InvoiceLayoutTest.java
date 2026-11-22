package einvoice.core;

import einvoice.TestData;
import einvoice.model.Company;
import einvoice.model.InvoiceData;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.geom.Rectangle2D;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the first page follows the German business letter standard DIN 5008 (form B), so the
 * address shows in the window of a standard envelope when an invoice is printed.
 */
class InvoiceLayoutTest {
    private static final double POINTS_PER_MM = 72 / 25.4;
    /** Text extraction decides by the centre of a letter, so areas get one millimetre of slack. */
    private static final double TOLERANCE = 1;

    private static PDDocument document;

    @BeforeAll
    static void renderInvoice() throws Exception {
        Company company = TestData.company();
        InvoiceData invoice = TestData.invoice(TestData.domesticCustomer());
        byte[] xml = XmlBuilder.xml(company, invoice);
        document = Loader.loadPDF(PdfBuilder.renderLayout(company, invoice, xml, null, null));
    }

    @Test
    void recipientAddressSitsInTheAddressZone() throws Exception {
        String addressZone = textIn(25 - TOLERANCE, 62.7, 80, 27.3);

        assertTrue(addressZone.contains("Beispielkunde GmbH"), addressZone);
        assertTrue(addressZone.contains("Kundenallee 12"), addressZone);
        assertTrue(addressZone.contains("14467 Potsdam"), addressZone);
    }

    @Test
    void returnAddressSitsAboveTheAddressZone() throws Exception {
        String returnAddressZone = textIn(25 - TOLERANCE, 45, 80, 17.7);

        assertTrue(returnAddressZone.contains("Musterfirma GmbH, Musterstraße 1, 10115 Berlin"), returnAddressZone);
        assertFalse(returnAddressZone.contains("Beispielkunde"), "the recipient starts below 62.7 mm");
    }

    @Test
    void informationBlockStartsAt125MillimetresFromTheLeft() throws Exception {
        String informationBlock = textIn(125 - TOLERANCE, 50, 65 + TOLERANCE, 40);
        String leftOfIt = textIn(105, 50, 20 - 2 * TOLERANCE, 40);

        assertTrue(informationBlock.contains("Rechnungsnummer"), informationBlock);
        assertTrue(informationBlock.contains("RE-2026-00043"), informationBlock);
        assertTrue(leftOfIt.isBlank(), "nothing between the address window and the information block");
    }

    @Test
    void letterTextStartsBelowTheHeaderAndInsideTheMargins() throws Exception {
        String header = textIn(0, 90.5, 210, 7);
        String body = textIn(25 - TOLERANCE, 98, 165 + TOLERANCE, 150);
        String leftMargin = textIn(0, 98, 25 - 2 * TOLERANCE, 150);
        String rightMargin = textIn(190 + TOLERANCE, 0, 20 - TOLERANCE, 297);

        assertTrue(header.isBlank(), "free line between address window and text: " + header);
        assertTrue(body.contains("Rechnung Nr. RE-2026-00043"), body);
        assertTrue(leftMargin.isBlank(), "left margin of 25 mm");
        assertTrue(rightMargin.isBlank(), "right margin of 20 mm");
    }

    @Test
    void footerAndInformationBlockShowTheCompanyDetails() throws Exception {
        Company company = TestData.company();
        company.setManagingDirector("Erika Mustermann");
        company.setRegisterCourt("Amtsgericht Musterstadt");
        company.setWeb("www.musterfirma.example");
        company.setFooterExtra("Mitglied im Musterverband");
        InvoiceData invoice = TestData.invoice(TestData.customer(einvoice.model.TaxCase.EU_REVERSE_CHARGE, "FR",
                "FR12345678901"));
        byte[] xml = XmlBuilder.xml(company, invoice);

        try (PDDocument detailed = Loader.loadPDF(PdfBuilder.renderLayout(company, invoice, xml, null, null))) {
            PDFTextStripperByArea stripper = new PDFTextStripperByArea();
            stripper.addRegion("footer", new Rectangle2D.Double(0, 265 * POINTS_PER_MM,
                    210 * POINTS_PER_MM, 32 * POINTS_PER_MM));
            stripper.addRegion("top", new Rectangle2D.Double(0, 0, 210 * POINTS_PER_MM, 97 * POINTS_PER_MM));
            stripper.extractRegions(detailed.getPage(0));
            // A long e-mail address may wrap after the @ to stay inside its column.
            String footer = stripper.getTextForRegion("footer").replaceAll("@\\s+", "@");
            String top = stripper.getTextForRegion("top");

            for (String expected : new String[] {"Geschäftsführung", "Erika Mustermann", "Amtsgericht Musterstadt",
                "HRB 00000", "Musterbank", "DE36000000000000000000", "TESTDEFFXXX", "T: +49 30 0000000",
                "rechnung@musterfirma.example", "www.musterfirma.example", "USt-IdNr. DE123456789",
                "Mitglied im Musterverband"}) {
                assertTrue(footer.contains(expected), expected + " is missing in the footer: " + footer);
            }
            for (String expected : new String[] {"Oktober 2026", "+49 30 0000000", "Erika Mustermann", "Frankreich",
                "FR12345678901", "17.11.2026"}) {
                assertTrue(top.contains(expected), expected + " is missing in the header: " + top);
            }
        }
    }

    /** Text of the first page inside a rectangle given in millimetres from the top left corner. */
    private static String textIn(double left, double top, double width, double height) throws Exception {
        PDFTextStripperByArea stripper = new PDFTextStripperByArea();
        stripper.addRegion("area", new Rectangle2D.Double(left * POINTS_PER_MM, top * POINTS_PER_MM,
                width * POINTS_PER_MM, height * POINTS_PER_MM));
        stripper.extractRegions(document.getPage(0));
        return stripper.getTextForRegion("area").strip();
    }
}
