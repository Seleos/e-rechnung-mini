package einvoice.service;

import einvoice.model.Company;
import einvoice.model.Customer;
import einvoice.model.InvoiceData;
import einvoice.model.LineItem;
import einvoice.model.Service;
import einvoice.storage.DataStore;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Prepares new invoices that are not created yet: from the services pre-filled for a customer, or as a copy of
 * an invoice created earlier. A draft gets the next free number and the month before the invoice date as its
 * service period; it still has to pass the input check before it can be created.
 */
public final class InvoiceDrafts {
    private final DataStore store;

    /**
     * Creates the use case on a data folder.
     *
     * @param store data folder that supplies numbers and current customer data
     */
    public InvoiceDrafts(DataStore store) {
        this.store = store;
    }

    /**
     * Starts an invoice with the services the customer has marked as pre-filled.
     *
     * @param company  issuing company
     * @param customer recipient
     * @param date     invoice date
     * @return a draft whose lines have no quantity yet
     */
    public InvoiceData forCustomer(Company company, Customer customer, LocalDate date) {
        InvoiceData draft = start(company, date);
        draft.setCustomer(customer.copy());
        draft.setItems(defaultLines(customer));
        return draft;
    }

    /**
     * Starts an invoice with the customer and the line items of an invoice created earlier.
     *
     * @param company  issuing company
     * @param template record of the earlier invoice
     * @param date     invoice date
     * @return a draft with copies of the template's lines, quantities included
     */
    public InvoiceData fromTemplate(Company company, InvoiceData template, LocalDate date) {
        InvoiceData draft = start(company, date);
        draft.setCustomer(currentCustomer(company, template.getCustomer()));
        List<LineItem> items = new ArrayList<>();
        for (LineItem item : template.getItems()) {
            items.add(new LineItem(item.getQuantity(), item.getUnit(), item.getText(), item.getUnitPrice()));
        }
        draft.setItems(items);
        return draft;
    }

    /**
     * Returns the lines a new invoice for this customer starts with.
     *
     * @param customer recipient, may be null
     * @return one line without quantity for every pre-filled service, empty if there are none
     */
    public static List<LineItem> defaultLines(Customer customer) {
        List<LineItem> lines = new ArrayList<>();
        if (customer == null) {
            return lines;
        }
        for (Service service : customer.getServices()) {
            if (service.isIncludedByDefault()) {
                lines.add(new LineItem(null, service.getUnit(), service.getText(), service.getUnitPrice()));
            }
        }
        return lines;
    }

    /**
     * Returns the service period a new invoice is proposed with: invoices are written after the work is done.
     *
     * @param date invoice date
     * @return the calendar month before the invoice date
     */
    public static YearMonth defaultServiceMonth(LocalDate date) {
        return YearMonth.from(date).minusMonths(1);
    }

    private InvoiceData start(Company company, LocalDate date) {
        InvoiceData draft = new InvoiceData();
        draft.setNumber(store.nextNumber(company, date));
        draft.setDate(date);
        YearMonth month = defaultServiceMonth(date);
        draft.setServiceFrom(month.atDay(1));
        draft.setServiceTo(month.atEndOfMonth());
        return draft;
    }

    /** The customer as stored now, so a changed address is used; a deleted customer comes from the copy. */
    private Customer currentCustomer(Company company, Customer copy) {
        if (copy == null) {
            return null;
        }
        return store.customers(company).stream()
                .filter(candidate -> candidate.getId().equals(copy.getId()))
                .findFirst()
                .orElse(copy)
                .copy();
    }
}
