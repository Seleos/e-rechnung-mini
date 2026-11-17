package einvoice.core;

import einvoice.i18n.InvoiceTexts;
import einvoice.model.Company;
import einvoice.model.InvoiceData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fills the e-mail template of a customer with the data of an invoice. The app does not send e-mails;
 * the finished subject and text are copied into the user's own mail program.
 */
public final class EmailText {
    private static final List<String> EMAIL_ONLY = List.of("{amount}", "{customer}", "{company}", "{footer}");

    /** Placeholders an e-mail template may contain: the general ones plus those only an e-mail needs. */
    public static final List<String> PLACEHOLDERS = placeholderNames();

    private EmailText() {
    }

    /**
     * Returns the subject of the e-mail for an invoice.
     *
     * @param company issuing company
     * @param invoice created invoice with totals
     * @return the customer's subject template, or the standard subject if it is empty, with all
     *         placeholders replaced
     */
    public static String subject(Company company, InvoiceData invoice) {
        return fill(template(invoice, invoice.getCustomer().getEmailSubject(), "email.subject"), company, invoice);
    }

    /**
     * Returns the text of the e-mail for an invoice.
     *
     * @param company issuing company
     * @param invoice created invoice with totals
     * @return the customer's text template, or the standard text if it is empty, with all placeholders
     *         replaced
     */
    public static String body(Company company, InvoiceData invoice) {
        return fill(template(invoice, invoice.getCustomer().getEmailBody(), "email.body"), company, invoice);
    }

    private static List<String> placeholderNames() {
        List<String> names = new ArrayList<>(Placeholders.NAMES);
        names.addAll(EMAIL_ONLY);
        return List.copyOf(names);
    }

    private static String template(InvoiceData invoice, String customerTemplate, String standardKey) {
        if (customerTemplate != null && !customerTemplate.isBlank()) {
            return customerTemplate;
        }
        return InvoiceTexts.of(invoice.getCustomer().getInvoiceLanguage()).get(standardKey);
    }

    private static String fill(String template, Company company, InvoiceData invoice) {
        Map<String, String> values = Placeholders.values(company, invoice);
        values.put("{amount}", amount(invoice));
        values.put("{customer}", invoice.getCustomer().getName());
        values.put("{company}", company.getName());
        values.put("{footer}", footer(company));
        return Placeholders.replace(template, values);
    }

    private static String amount(InvoiceData invoice) {
        if (invoice.getGross() == null) {
            return "";
        }
        return Numbers.money(invoice.getGross(), invoice.getCustomer().getCurrency());
    }

    /** Signature block built from the company master data; empty lines are left out. */
    private static String footer(Company company) {
        List<String> lines = new ArrayList<>();
        for (String line : new String[] {company.getContact(), company.getName(), company.getStreet(),
            (company.getZip() + " " + company.getCity()).trim(), company.getPhone(), company.getEmail(),
            company.getWeb()}) {
            if (line != null && !line.isBlank()) {
                lines.add(line.trim());
            }
        }
        return String.join("\n", lines);
    }
}
