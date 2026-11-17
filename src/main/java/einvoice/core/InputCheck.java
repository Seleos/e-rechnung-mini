package einvoice.core;

import einvoice.i18n.Messages;
import einvoice.model.Company;
import einvoice.model.Customer;
import einvoice.model.InvoiceData;
import einvoice.model.LineItem;
import einvoice.model.TaxCase;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.regex.Pattern;

/** Checks before generation that all mandatory details are present. Messages are shown to the user. */
public final class InputCheck {
    /** The number becomes the file name, so it must not contain characters that are illegal in file names. */
    private static final String FORBIDDEN = "/\\\\:*?\"<>|\\p{Cntrl}";
    private static final Pattern FILE_SAFE_NUMBER =
            Pattern.compile("[^" + FORBIDDEN + "]*[^" + FORBIDDEN + ". ]");

    private InputCheck() {
    }

    /**
     * Lists everything that is missing or wrong before an invoice can be generated.
     *
     * @param company issuing company
     * @param invoice invoice data as entered
     * @return messages for the user in the selected language, empty if the invoice is complete
     */
    public static List<String> errors(Company company, InvoiceData invoice) {
        List<String> errors = new ArrayList<>();
        checkCompany(company, errors);
        checkCustomer(company, invoice.getCustomer(), errors);
        checkHeader(invoice, errors);
        checkItems(invoice.getItems(), errors);
        return errors;
    }

    private static void checkCompany(Company company, List<String> errors) {
        if (blank(company.getName())) {
            errors.add(Messages.get("check.company.name"));
        }
        if (blank(company.getStreet()) || blank(company.getZip()) || blank(company.getCity())) {
            errors.add(Messages.get("check.company.address"));
        }
        if (blank(company.getVatId()) && blank(company.getTaxNumber())) {
            errors.add(Messages.get("check.company.taxId"));
        }
        if (blank(company.getIban())) {
            errors.add(Messages.get("check.company.iban"));
        }
    }

    private static void checkCustomer(Company company, Customer customer, List<String> errors) {
        if (customer == null || blank(customer.getName())) {
            errors.add(Messages.get("check.customer.none"));
            return;
        }
        if (blank(customer.getStreet()) || blank(customer.getZip()) || blank(customer.getCity())) {
            errors.add(Messages.get("check.customer.address"));
        }
        if (blank(customer.getCountry()) || customer.getCountry().trim().length() != 2) {
            errors.add(Messages.get("check.customer.country"));
        }
        checkCurrency(company, customer, errors);
        if (company.isSmallBusiness()) {
            return;
        }
        if (customer.getTaxCase() == TaxCase.EU_REVERSE_CHARGE) {
            if (blank(customer.getVatId())) {
                errors.add(Messages.get("check.customer.vatId"));
            }
            if (blank(company.getVatId())) {
                errors.add(Messages.get("check.company.vatId"));
            }
        }
        if (customer.getTaxCase() == TaxCase.THIRD_COUNTRY && blank(company.getTaxNumber())) {
            errors.add(Messages.get("check.company.taxNumber"));
        }
    }

    /**
     * German VAT has to be stated in euro. Converting a foreign currency would need an exchange rate, so
     * invoices with VAT are limited to euro; invoices without VAT may use any currency.
     */
    private static void checkCurrency(Company company, Customer customer, List<String> errors) {
        try {
            Currency.getInstance(customer.getCurrency());
        } catch (IllegalArgumentException e) {
            errors.add(Messages.get("check.customer.currency"));
            return;
        }
        boolean euro = Customer.DEFAULT_CURRENCY.equals(customer.getCurrency());
        if (!euro && Tax.of(company, customer).standard()) {
            errors.add(Messages.get("check.currency.vat"));
        }
    }

    private static void checkHeader(InvoiceData invoice, List<String> errors) {
        if (blank(invoice.getNumber())) {
            errors.add(Messages.get("check.number"));
        } else if (!FILE_SAFE_NUMBER.matcher(invoice.getNumber().trim()).matches()
                || invoice.getNumber().contains("..")) {
            errors.add(Messages.get("check.numberCharacters"));
        }
        if (invoice.getDate() == null) {
            errors.add(Messages.get("check.date"));
        }
        if (invoice.getServiceFrom() == null || invoice.getServiceTo() == null) {
            errors.add(Messages.get("check.period"));
        } else if (invoice.getServiceTo().isBefore(invoice.getServiceFrom())) {
            errors.add(Messages.get("check.periodOrder"));
        }
    }

    private static void checkItems(List<LineItem> items, List<String> errors) {
        if (items.isEmpty()) {
            errors.add(Messages.get("check.noItems"));
        }
        int position = 1;
        for (LineItem item : items) {
            String prefix = Messages.get("check.item.prefix", position) + " ";
            if (blank(item.getText())) {
                errors.add(prefix + Messages.get("check.item.text"));
            }
            if (item.getQuantity() == null || item.getQuantity().signum() <= 0) {
                errors.add(prefix + Messages.get("check.item.quantity"));
            }
            if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                errors.add(prefix + Messages.get("check.item.price"));
            }
            position++;
        }
    }

    private static boolean blank(String text) {
        return text == null || text.isBlank();
    }
}
