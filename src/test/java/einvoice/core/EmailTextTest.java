package einvoice.core;

import einvoice.TestData;
import einvoice.i18n.Language;
import einvoice.model.Company;
import einvoice.model.Customer;
import einvoice.model.InvoiceData;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests filling the e-mail template of a customer with the data of an invoice. */
class EmailTextTest {
    @Test
    void customerTemplateIsFilledWithInvoiceData() {
        Customer customer = TestData.domesticCustomer();
        customer.setEmailSubject("Rechnung {number} {month} {year}");
        customer.setEmailBody("Betrag {amount}, fällig {due}, Zeitraum {period}, an {customer} von {company}.");

        InvoiceData invoice = createdInvoice(customer);

        assertEquals("Rechnung RE-2026-00043 Oktober 2026", EmailText.subject(TestData.company(), invoice));
        assertEquals("Betrag 562,28 €, fällig 17.12.2026, Zeitraum Oktober 2026, "
                + "an Beispielkunde GmbH von Musterfirma GmbH.", EmailText.body(TestData.company(), invoice));
    }

    @Test
    void footerIsBuiltFromCompanyDataWithoutEmptyLines() {
        Customer customer = TestData.domesticCustomer();
        customer.setEmailBody("{footer}");
        Company company = TestData.company();
        company.setWeb("");

        String footer = EmailText.body(company, createdInvoice(customer));

        assertEquals("Erika Mustermann\nMusterfirma GmbH\nMusterstraße 1\n10115 Berlin\n+49 30 0000000\n"
                + "rechnung@musterfirma.example", footer);
    }

    @Test
    void emptyTemplateFallsBackToTheStandardTextInTheInvoiceLanguage() {
        Customer german = TestData.domesticCustomer();
        Customer english = TestData.domesticCustomer();
        english.setInvoiceLanguage(Language.ENGLISH);

        assertEquals("Rechnung RE-2026-00043 für Oktober 2026",
                EmailText.subject(TestData.company(), createdInvoice(german)));
        assertEquals("Invoice RE-2026-00043 for October 2026",
                EmailText.subject(TestData.company(), createdInvoice(english)));
        String englishBody = EmailText.body(TestData.company(), createdInvoice(english));
        assertTrue(englishBody.startsWith("Hello,"));
        assertTrue(englishBody.contains("Payable by 17 December 2026."), englishBody);
    }

    @Test
    void standardTextLeavesNoPlaceholderBehind() {
        for (Language language : Language.values()) {
            Customer customer = TestData.domesticCustomer();
            customer.setInvoiceLanguage(language);
            String body = EmailText.body(TestData.company(), createdInvoice(customer));
            assertFalse(body.contains("{"), language + ": " + body);
        }
    }

    private static InvoiceData createdInvoice(Customer customer) {
        InvoiceData invoice = TestData.invoice(customer);
        invoice.setGross(new BigDecimal("562.28"));
        return invoice;
    }
}
