package einvoice.core;

import einvoice.i18n.InvoiceTexts;
import einvoice.model.Company;
import einvoice.model.InvoiceData;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Placeholders that can be written into line item texts, the text below the invoice and the e-mail
 * template. They are replaced when an invoice is generated, in the invoice language of the customer.
 */
public final class Placeholders {
    /** Placeholders available everywhere, in the order they are documented for the user. */
    public static final List<String> NAMES = List.of(
            "{number}", "{today}", "{due}", "{month}", "{year}", "{period}",
            "{currentMonth}", "{currentYear}", "{lastMonth}", "{lastMonthYear}",
            "{firstDayOfMonth}", "{lastDayOfMonth}", "{firstDayOfLastMonth}", "{lastDayOfLastMonth}");

    private Placeholders() {
    }

    /**
     * Replaces all placeholders in a text.
     *
     * @param text    text that may contain placeholders, may be null
     * @param company issuing company, source of the payment term
     * @param invoice invoice the text belongs to
     * @return the text with all known placeholders replaced, or an empty text for null
     */
    public static String fill(String text, Company company, InvoiceData invoice) {
        if (text == null) {
            return "";
        }
        return replace(text, values(company, invoice));
    }

    /**
     * Replaces the given placeholders in a text.
     *
     * @param text   text that may contain placeholders
     * @param values placeholder names with their values
     * @return the text with these placeholders replaced
     */
    static String replace(String text, Map<String, String> values) {
        String result = text;
        for (Map.Entry<String, String> value : values.entrySet()) {
            result = result.replace(value.getKey(), value.getValue());
        }
        return result;
    }

    /**
     * Calculates the value of every placeholder for an invoice.
     *
     * @param company issuing company
     * @param invoice invoice the values are derived from
     * @return placeholder names with their values, in documentation order
     */
    static Map<String, String> values(Company company, InvoiceData invoice) {
        InvoiceTexts texts = InvoiceTexts.of(
                invoice.getCustomer() == null ? null : invoice.getCustomer().getInvoiceLanguage());
        LocalDate today = invoice.getDate() == null ? LocalDate.now() : invoice.getDate();
        LocalDate lastMonth = today.minusMonths(1);
        // "Month" and "year" describe the service period, because that is what the invoice is about.
        LocalDate service = invoice.getServiceFrom() == null ? today : invoice.getServiceFrom();

        Map<String, String> values = new LinkedHashMap<>();
        values.put("{number}", invoice.getNumber() == null ? "" : invoice.getNumber());
        values.put("{today}", texts.date(today));
        values.put("{due}", texts.date(today.plusDays(company.getPaymentDays())));
        values.put("{month}", texts.month(service));
        values.put("{year}", String.valueOf(service.getYear()));
        values.put("{period}", texts.period(invoice.getServiceFrom(), invoice.getServiceTo()));
        values.put("{currentMonth}", texts.month(today));
        values.put("{currentYear}", String.valueOf(today.getYear()));
        values.put("{lastMonthYear}", String.valueOf(lastMonth.getYear()));
        values.put("{lastMonth}", texts.month(lastMonth));
        values.put("{firstDayOfMonth}", texts.date(today.withDayOfMonth(1)));
        values.put("{lastDayOfMonth}", texts.date(today.withDayOfMonth(today.lengthOfMonth())));
        values.put("{firstDayOfLastMonth}", texts.date(lastMonth.withDayOfMonth(1)));
        values.put("{lastDayOfLastMonth}", texts.date(lastMonth.withDayOfMonth(lastMonth.lengthOfMonth())));
        return values;
    }
}
