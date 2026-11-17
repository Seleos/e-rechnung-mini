package einvoice;

import einvoice.model.Company;
import einvoice.model.Customer;
import einvoice.model.InvoiceData;
import einvoice.model.LineItem;
import einvoice.model.TaxCase;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Fictitious, complete test fixtures shared by the tests. */
public final class TestData {
    private TestData() {
    }

    /**
     * Creates a company with all details needed for a valid invoice.
     *
     * @return an unsaved company
     */
    public static Company company() {
        Company company = new Company();
        company.setName("Musterfirma GmbH");
        company.setStreet("Musterstraße 1");
        company.setZip("10115");
        company.setCity("Berlin");
        company.setPhone("+49 30 0000000");
        company.setEmail("rechnung@musterfirma.example");
        company.setContact("Erika Mustermann");
        company.setRegisterNumber("HRB 00000");
        company.setVatId("DE123456789");
        company.setTaxNumber("00/000/00000");
        company.setBankName("Musterbank");
        company.setIban("DE36 0000 0000 0000 0000 00");
        company.setBic("TESTDEFFXXX");
        return company;
    }

    /**
     * Creates a customer with a complete address.
     *
     * @param taxCase tax case of the customer
     * @param country two-letter country code
     * @param vatId   VAT ID, may be empty
     * @return an unsaved customer
     */
    public static Customer customer(TaxCase taxCase, String country, String vatId) {
        Customer customer = new Customer();
        customer.setName("Beispielkunde GmbH");
        customer.setStreet("Kundenallee 12");
        customer.setZip("14467");
        customer.setCity("Potsdam");
        customer.setCountry(country);
        customer.setVatId(vatId);
        customer.setTaxCase(taxCase);
        return customer;
    }

    /**
     * Creates a domestic customer taxed at 19 %.
     *
     * @return an unsaved customer
     */
    public static Customer domesticCustomer() {
        return customer(TaxCase.DOMESTIC_19, "DE", "DE111111111");
    }

    /**
     * Creates an invoice with one line of 6.75 hours at 70.00 euro for October 2026.
     *
     * @param customer recipient
     * @return a complete invoice
     */
    public static InvoiceData invoice(Customer customer) {
        InvoiceData invoice = new InvoiceData();
        invoice.setNumber("RE-2026-00043");
        invoice.setDate(LocalDate.of(2026, 11, 17));
        invoice.setServiceFrom(LocalDate.of(2026, 10, 1));
        invoice.setServiceTo(LocalDate.of(2026, 10, 31));
        invoice.setCustomer(customer);
        invoice.getItems().add(new LineItem(new BigDecimal("6.75"), "h",
                "Wartung der Kundendatenbank\nLeistungszeitraum: Oktober 2026", new BigDecimal("70.00")));
        return invoice;
    }
}
