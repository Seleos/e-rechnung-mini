package einvoice.core;

import einvoice.TestData;
import einvoice.i18n.Language;
import einvoice.model.Company;
import einvoice.model.Customer;
import einvoice.model.InvoiceData;
import einvoice.model.LineItem;
import einvoice.model.TaxCase;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the totals and the tax relevant content of the generated invoice XML. */
class XmlBuilderTest {
    @Test
    void calculatesTotalsWithVat() {
        XmlBuilder.Totals totals = XmlBuilder.totals(TestData.company(),
                TestData.invoice(TestData.domesticCustomer()));

        assertEquals(new BigDecimal("472.50"), totals.net());
        assertEquals(new BigDecimal("89.78"), totals.tax());
        assertEquals(new BigDecimal("562.28"), totals.gross());
    }

    @Test
    void sumsSeveralLines() {
        InvoiceData invoice = TestData.invoice(TestData.domesticCustomer());
        invoice.getItems().add(new LineItem(new BigDecimal("2"), "Stk", "Hosting", new BigDecimal("49.00")));

        XmlBuilder.Totals totals = XmlBuilder.totals(TestData.company(), invoice);

        assertEquals(new BigDecimal("570.50"), totals.net());
        assertEquals(new BigDecimal("678.90"), totals.gross());
    }

    @Test
    void chargesNoVatForReverseCharge() {
        XmlBuilder.Totals totals = XmlBuilder.totals(TestData.company(),
                TestData.invoice(TestData.customer(TaxCase.EU_REVERSE_CHARGE, "FR", "FR12345678901")));

        assertEquals(0, totals.tax().signum());
        assertEquals(totals.net(), totals.gross());
    }

    @Test
    void invoiceWithoutLinesHasZeroTotals() {
        InvoiceData invoice = TestData.invoice(TestData.domesticCustomer());
        invoice.getItems().clear();

        XmlBuilder.Totals totals = XmlBuilder.totals(TestData.company(), invoice);

        assertEquals(BigDecimal.ZERO, totals.net());
        assertEquals(BigDecimal.ZERO, totals.gross());
    }

    @Test
    void xmlCarriesNumberPartiesAndItem() {
        String xml = xml(TestData.company(), TestData.invoice(TestData.domesticCustomer()));

        assertTrue(xml.contains("<ram:ID>RE-2026-00043</ram:ID>"));
        assertTrue(xml.contains("Musterfirma GmbH"));
        assertTrue(xml.contains("Beispielkunde GmbH"));
        assertTrue(xml.contains("<ram:Name>Wartung der Kundendatenbank</ram:Name>"));
        assertTrue(xml.contains("DE36000000000000000000"), "IBAN is written without spaces");
        assertTrue(xml.contains("unitCode=\"HUR\""));
    }

    @Test
    void reverseChargeUsesCategoryAeWithNote() {
        String xml = xml(TestData.company(),
                TestData.invoice(TestData.customer(TaxCase.EU_REVERSE_CHARGE, "FR", "FR12345678901")));

        assertTrue(xml.contains("<ram:CategoryCode>AE</ram:CategoryCode>"));
        assertTrue(xml.contains("Steuerschuldnerschaft des Leistungsempfängers"));
        assertTrue(xml.contains("FR12345678901"));
    }

    @Test
    void thirdCountryInvoiceCarriesNoVatId() {
        String xml = xml(TestData.company(),
                TestData.invoice(TestData.customer(TaxCase.THIRD_COUNTRY, "US", "US-IGNORED")));

        assertTrue(xml.contains("<ram:CategoryCode>O</ram:CategoryCode>"));
        assertFalse(xml.contains("schemeID=\"VA\""), "category O forbids VAT IDs");
        assertFalse(xml.contains("US-IGNORED"));
    }

    @Test
    void smallBusinessUsesExemptCategoryWithLegalNote() {
        Company smallBusiness = TestData.company();
        smallBusiness.setSmallBusiness(true);

        String xml = xml(smallBusiness, TestData.invoice(TestData.domesticCustomer()));

        assertTrue(xml.contains("<ram:CategoryCode>E</ram:CategoryCode>"));
        assertTrue(xml.contains("§ 19 UStG"));
    }

    @Test
    void englishInvoiceUsesEnglishNoteAndPaymentText() {
        Company company = TestData.company();
        company.setPaymentText("Zahlbar in 30 Tagen.");
        company.setPaymentTextEnglish("Payable within 30 days.");
        Customer customer = TestData.customer(TaxCase.EU_REVERSE_CHARGE, "FR", "FR12345678901");
        customer.setInvoiceLanguage(Language.ENGLISH);

        String xml = xml(company, TestData.invoice(customer));

        assertTrue(xml.contains("Reverse charge: the recipient of the service is liable for VAT."));
        assertTrue(xml.contains("Payable within 30 days."));
        assertFalse(xml.contains("Zahlbar in 30 Tagen."));
    }

    @Test
    void invoiceCarriesTheCurrencyOfTheCustomer() {
        Customer customer = TestData.customer(TaxCase.THIRD_COUNTRY, "US", "");
        customer.setCurrency("USD");

        String xml = xml(TestData.company(), TestData.invoice(customer));

        assertTrue(xml.contains("<ram:InvoiceCurrencyCode>USD</ram:InvoiceCurrencyCode>"));
        assertFalse(xml.contains("EUR"));
    }

    @Test
    void placeholdersInItemAndPaymentTextAreReplaced() {
        Company company = TestData.company();
        company.setPaymentText("Zahlbar bis {due} unter Angabe von {number}.");
        InvoiceData invoice = TestData.invoice(TestData.domesticCustomer());
        invoice.getItems().get(0).setText("Wartung {month} {year}\nLeistungszeitraum: {period}");

        String xml = xml(company, invoice);

        assertTrue(xml.contains("<ram:Name>Wartung Oktober 2026</ram:Name>"));
        assertTrue(xml.contains("Leistungszeitraum: Oktober 2026"));
        assertTrue(xml.contains("Zahlbar bis 17.12.2026 unter Angabe von RE-2026-00043."));
        assertFalse(xml.contains("{"));
    }

    @Test
    void unitPriceKeepsItsDecimalsAndStandardPaymentTextIsFilled() {
        Company company = TestData.company();
        company.useStandardPaymentTexts();
        InvoiceData invoice = TestData.invoice(TestData.domesticCustomer());
        invoice.getItems().get(0).setUnitPrice(new BigDecimal("0.1234"));

        String xml = xml(company, invoice);

        assertTrue(xml.contains("0.1234"));
        assertTrue(xml.contains("bis zum 17.12.2026"));
        assertTrue(xml.contains("Rechnungsnummer RE-2026-00043"));
    }

    @Test
    void companyWithoutPaymentTextGetsTheStandardWordingInTheInvoiceLanguage() {
        Customer english = TestData.domesticCustomer();
        english.setInvoiceLanguage(Language.ENGLISH);

        String germanXml = xml(TestData.company(), TestData.invoice(TestData.domesticCustomer()));
        String englishXml = xml(TestData.company(), TestData.invoice(english));

        assertTrue(germanXml.contains("bis zum 17.12.2026"), "German standard text");
        assertTrue(englishXml.contains("by 17 December 2026"), "English standard text");
        assertFalse(germanXml.contains("Please remit"), "no text inserted by the library");
    }

    @Test
    void servicePeriodIsWrittenOnlyWhenBothDatesAreKnown() {
        InvoiceData withPeriod = TestData.invoice(TestData.domesticCustomer());
        InvoiceData withoutPeriod = TestData.invoice(TestData.domesticCustomer());
        withoutPeriod.setServiceFrom(null);

        String periodXml = xml(TestData.company(), withPeriod);
        String noPeriodXml = xml(TestData.company(), withoutPeriod);

        assertTrue(periodXml.contains("<ram:BillingSpecifiedPeriod>"));
        assertTrue(periodXml.contains("20261001"));
        assertTrue(periodXml.contains("20261031"));
        assertFalse(noPeriodXml.contains("<ram:BillingSpecifiedPeriod>"));
        assertTrue(noPeriodXml.contains("20261117"), "the invoice date serves as delivery date");
    }

    @Test
    void optionalBuyerDetailsAppearOnlyWhenFilled() {
        Customer plain = TestData.customer(TaxCase.DOMESTIC_19, "DE", "");
        Customer detailed = TestData.domesticCustomer();
        detailed.setAddressExtra("Gebäude C");
        detailed.setEmail("buchhaltung@beispielkunde.example");
        detailed.setBuyerReference("04011000-1234512345-06");

        String plainXml = xml(TestData.company(), TestData.invoice(plain));
        String detailedXml = xml(TestData.company(), TestData.invoice(detailed));

        assertTrue(detailedXml.contains("Gebäude C"));
        assertTrue(detailedXml.contains("buchhaltung@beispielkunde.example"));
        assertTrue(detailedXml.contains("<ram:BuyerReference>04011000-1234512345-06</ram:BuyerReference>"));
        assertTrue(detailedXml.contains("DE111111111"));
        assertFalse(plainXml.contains("<ram:BuyerReference>"));
        assertFalse(plainXml.contains("<ram:LineTwo>"));
        assertFalse(plainXml.contains("DE111111111"));
    }

    @Test
    void sellerDetailsAppearOnlyWhenFilled() {
        Company minimal = TestData.company();
        minimal.setContact("");
        minimal.setPhone("");
        minimal.setEmail("");
        minimal.setBic("");
        minimal.setRegisterNumber("");
        minimal.setTaxNumber("");
        minimal.setCountry("");

        String fullXml = xml(TestData.company(), TestData.invoice(TestData.domesticCustomer()));
        String minimalXml = xml(minimal, TestData.invoice(TestData.domesticCustomer()));

        assertTrue(fullXml.contains("<ram:PersonName>Erika Mustermann</ram:PersonName>"));
        assertTrue(fullXml.contains("+49 30 0000000"));
        assertTrue(fullXml.contains("rechnung@musterfirma.example"));
        assertTrue(fullXml.contains("TESTDEFFXXX"));
        assertTrue(fullXml.contains("HRB 00000"));
        assertTrue(fullXml.contains("<ram:ID schemeID=\"FC\">00/000/00000</ram:ID>"));
        assertTrue(fullXml.contains("<ram:ID schemeID=\"VA\">DE123456789</ram:ID>"));
        assertFalse(minimalXml.contains("<ram:DefinedTradeContact>"));
        assertFalse(minimalXml.contains("TESTDEFFXXX"));
        assertFalse(minimalXml.contains("HRB 00000"));
        assertFalse(minimalXml.contains("schemeID=\"FC\""));
        assertTrue(minimalXml.contains("<ram:CountryID>DE</ram:CountryID>"), "a missing country means Germany");
    }

    @Test
    void sellerWithoutVatIdAndRegisterNumberIsIdentifiedByTheTaxNumber() {
        Company soleProprietor = TestData.company();
        soleProprietor.setSmallBusiness(true);
        soleProprietor.setVatId("");
        soleProprietor.setRegisterNumber("");
        Company withVatId = TestData.company();
        withVatId.setRegisterNumber("");

        String sellerOf = "<ram:SellerTradeParty>";
        String proprietorXml = xml(soleProprietor, TestData.invoice(TestData.domesticCustomer()));
        String vatIdXml = xml(withVatId, TestData.invoice(TestData.domesticCustomer()));

        assertTrue(proprietorXml.contains(sellerOf + "<ram:ID>00/000/00000</ram:ID>")
                || proprietorXml.replaceAll("\\s+", "").contains(sellerOf + "<ram:ID>00/000/00000</ram:ID>"));
        assertFalse(vatIdXml.replaceAll("\\s+", "").contains(sellerOf + "<ram:ID>00/000/00000</ram:ID>"));
    }

    @Test
    void invoiceDeclaresTheEn16931Profile() {
        String xml = xml(TestData.company(), TestData.invoice(TestData.domesticCustomer()));

        assertTrue(xml.contains("<ram:ID>urn:cen.eu:en16931:2017</ram:ID>"), "profile EN 16931, not basic or extended");
    }

    @Test
    void singleLineTextHasNoDescriptionAndFurtherLinesBecomeTheDescription() {
        InvoiceData singleLine = TestData.invoice(TestData.domesticCustomer());
        singleLine.getItems().get(0).setText("Wartung");
        InvoiceData multiLine = TestData.invoice(TestData.domesticCustomer());
        multiLine.getItems().get(0).setText("\nWartung\nZeile zwei\nZeile drei");

        String singleXml = xml(TestData.company(), singleLine);
        String multiXml = xml(TestData.company(), multiLine);

        assertTrue(singleXml.contains("<ram:Name>Wartung</ram:Name>"));
        assertFalse(product(singleXml).matches("(?s).*<ram:Description>[^<]+</ram:Description>.*"),
                "a single line leaves the description empty");
        assertTrue(multiXml.contains("<ram:Name>Wartung</ram:Name>"));
        assertTrue(multiXml.contains("Zeile zwei"));
        assertTrue(multiXml.contains("Zeile drei"));
    }

    @Test
    void englishInvoiceFallsBackToTheGermanPaymentText() {
        Company company = TestData.company();
        company.setPaymentText("Zahlbar in 30 Tagen.");
        Customer customer = TestData.domesticCustomer();
        customer.setInvoiceLanguage(Language.ENGLISH);

        assertTrue(xml(company, TestData.invoice(customer)).contains("Zahlbar in 30 Tagen."));
    }

    /** The part of the XML that describes the product of the first line. */
    private static String product(String xml) {
        return xml.substring(xml.indexOf("<ram:SpecifiedTradeProduct>"), xml.indexOf("</ram:SpecifiedTradeProduct>"));
    }

    private static String xml(Company company, InvoiceData invoice) {
        return new String(XmlBuilder.xml(company, invoice), StandardCharsets.UTF_8);
    }
}
