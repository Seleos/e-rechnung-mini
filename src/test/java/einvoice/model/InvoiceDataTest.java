package einvoice.model;

import einvoice.i18n.InvoiceTexts;
import einvoice.i18n.Language;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Tests how the service period is described on the invoice. */
class InvoiceDataTest {
    @Test
    void fullMonthIsNamed() {
        assertEquals("Oktober 2026", period(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)));
        assertEquals("Februar 2028", period(LocalDate.of(2028, 2, 1), LocalDate.of(2028, 2, 29)));
    }

    @Test
    void monthNameFollowsTheInvoiceLanguage() {
        InvoiceData invoice = new InvoiceData();
        invoice.setServiceFrom(LocalDate.of(2026, 10, 1));
        invoice.setServiceTo(LocalDate.of(2026, 10, 31));

        assertEquals("October 2026",
                InvoiceTexts.of(Language.ENGLISH).period(invoice.getServiceFrom(), invoice.getServiceTo()));
    }

    @Test
    void englishPeriodUsesEnglishDates() {
        InvoiceTexts english = InvoiceTexts.of(Language.ENGLISH);

        assertEquals("1 October 2026 – 15 October 2026",
                english.period(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15)));
        assertEquals("17 November 2026", english.date(LocalDate.of(2026, 11, 17)));
    }

    @Test
    void partialPeriodShowsBothDates() {
        assertEquals("01.10.2026 – 15.10.2026", period(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15)));
        assertEquals("15.09.2026 – 14.10.2026", period(LocalDate.of(2026, 9, 15), LocalDate.of(2026, 10, 14)));
    }

    @Test
    void singleDayShowsOneDate() {
        assertEquals("05.10.2026", period(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 5)));
    }

    @Test
    void missingPeriodIsEmpty() {
        assertEquals("", period(null, LocalDate.of(2026, 10, 5)));
        assertEquals("", period(LocalDate.of(2026, 10, 5), null));
    }

    @Test
    void customerCopyIsIndependent() {
        Customer original = new Customer();
        original.setName("Beispielkunde GmbH");
        original.getServices().add(new Service("Wartung", "h", new java.math.BigDecimal("70.00")));

        Customer copy = original.copy();
        copy.setName("Geändert");
        copy.getServices().get(0).setText("Geändert");

        assertEquals("Beispielkunde GmbH", original.getName());
        assertEquals("Wartung", original.getServices().get(0).getText());
    }

    private static String period(LocalDate from, LocalDate to) {
        return InvoiceTexts.of(Language.GERMAN).period(from, to);
    }
}
