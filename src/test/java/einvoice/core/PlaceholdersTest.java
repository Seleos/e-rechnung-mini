package einvoice.core;

import einvoice.TestData;
import einvoice.i18n.Language;
import einvoice.model.Customer;
import einvoice.model.InvoiceData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Tests the placeholders for line item texts, the text below the invoice and the e-mail template. */
class PlaceholdersTest {
    @Test
    void servicePeriodPlaceholdersDescribeTheBilledMonth() {
        assertEquals("Wartung Oktober 2026", fill("Wartung {month} {year}", Language.GERMAN));
        assertEquals("Leistungszeitraum: Oktober 2026", fill("Leistungszeitraum: {period}", Language.GERMAN));
    }

    @Test
    void datePlaceholdersAreRelativeToTheInvoiceDate() {
        assertEquals("17.11.2026", fill("{today}", Language.GERMAN));
        assertEquals("November 2026", fill("{currentMonth} {currentYear}", Language.GERMAN));
        assertEquals("Oktober 2026", fill("{lastMonth} {lastMonthYear}", Language.GERMAN));
        assertEquals("01.11.2026 bis 30.11.2026", fill("{firstDayOfMonth} bis {lastDayOfMonth}", Language.GERMAN));
        assertEquals("01.10.2026 bis 31.10.2026",
                fill("{firstDayOfLastMonth} bis {lastDayOfLastMonth}", Language.GERMAN));
    }

    @Test
    void numberAndDueDateAreAvailable() {
        assertEquals("RE-2026-00043, zahlbar bis 17.12.2026", fill("{number}, zahlbar bis {due}", Language.GERMAN));
    }

    @Test
    void valuesFollowTheInvoiceLanguage() {
        assertEquals("Maintenance October 2026", fill("Maintenance {month} {year}", Language.ENGLISH));
        assertEquals("payable by 17 December 2026", fill("payable by {due}", Language.ENGLISH));
        assertEquals("31 October 2026", fill("{lastDayOfLastMonth}", Language.ENGLISH));
    }

    @Test
    void lastMonthOfJanuaryIsDecemberOfThePreviousYear() {
        InvoiceData invoice = TestData.invoice(TestData.domesticCustomer());
        invoice.setDate(java.time.LocalDate.of(2027, 1, 5));

        assertEquals("Dezember 2026", Placeholders.fill("{lastMonth} {lastMonthYear}", TestData.company(), invoice));
    }

    @Test
    void everyDocumentedPlaceholderIsReplaced() {
        String all = String.join(" ", Placeholders.NAMES);

        assertFalse(fill(all, Language.GERMAN).contains("{"));
    }

    @Test
    void unknownPlaceholdersAndMissingTextAreLeftAlone() {
        assertEquals("{unknown}", fill("{unknown}", Language.GERMAN));
        assertEquals("", Placeholders.fill(null, TestData.company(), TestData.invoice(TestData.domesticCustomer())));
    }

    private static String fill(String text, Language language) {
        Customer customer = TestData.domesticCustomer();
        customer.setInvoiceLanguage(language);
        return Placeholders.fill(text, TestData.company(), TestData.invoice(customer));
    }
}
