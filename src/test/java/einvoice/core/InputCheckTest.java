package einvoice.core;

import einvoice.TestData;
import einvoice.i18n.Language;
import einvoice.i18n.Messages;
import einvoice.model.Company;
import einvoice.model.Customer;
import einvoice.model.InvoiceData;
import einvoice.model.LineItem;
import einvoice.model.TaxCase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests that incomplete invoices are rejected with a message naming what is missing. */
class InputCheckTest {
    @BeforeAll
    static void useGerman() {
        Messages.setLanguage(Language.GERMAN);
    }

    @Test
    void completeInvoiceHasNoErrors() {
        assertEquals(List.of(), InputCheck.errors(TestData.company(), TestData.invoice(TestData.domesticCustomer())));
    }

    @Test
    void reportsMissingCompanyDetails() {
        Company company = TestData.company();
        company.setName("");
        company.setCity(" ");
        company.setVatId("");
        company.setTaxNumber("");
        company.setIban("");

        List<String> errors = InputCheck.errors(company, TestData.invoice(TestData.domesticCustomer()));

        assertEquals(List.of(Messages.get("check.company.name"), Messages.get("check.company.address"),
                Messages.get("check.company.taxId"), Messages.get("check.company.iban")), errors);
    }

    @Test
    void reportsMissingCustomer() {
        InvoiceData invoice = TestData.invoice(null);
        assertEquals(List.of(Messages.get("check.customer.none")), InputCheck.errors(TestData.company(), invoice));
    }

    @Test
    void reportsIncompleteCustomerAddressAndCountry() {
        Customer customer = TestData.domesticCustomer();
        customer.setZip("");
        customer.setCountry("Deutschland");

        List<String> errors = InputCheck.errors(TestData.company(), TestData.invoice(customer));

        assertEquals(List.of(Messages.get("check.customer.address"), Messages.get("check.customer.country")), errors);
    }

    @Test
    void reverseChargeNeedsBothVatIds() {
        Company company = TestData.company();
        company.setVatId("");
        Customer customer = TestData.customer(TaxCase.EU_REVERSE_CHARGE, "FR", "");

        List<String> errors = InputCheck.errors(company, TestData.invoice(customer));

        assertEquals(List.of(Messages.get("check.customer.vatId"), Messages.get("check.company.vatId")), errors);
    }

    @Test
    void thirdCountryNeedsTheTaxNumber() {
        Company company = TestData.company();
        company.setTaxNumber("");
        Customer customer = TestData.customer(TaxCase.THIRD_COUNTRY, "US", "");

        assertEquals(List.of(Messages.get("check.company.taxNumber")),
                InputCheck.errors(company, TestData.invoice(customer)));
    }

    @Test
    void smallBusinessNeedsNoVatIdsForForeignCustomers() {
        Company company = TestData.company();
        company.setSmallBusiness(true);
        company.setVatId("");
        Customer customer = TestData.customer(TaxCase.EU_REVERSE_CHARGE, "FR", "");

        assertEquals(List.of(), InputCheck.errors(company, TestData.invoice(customer)));
    }

    @Test
    void invoicesWithVatMustBeInEuro() {
        Customer customer = TestData.domesticCustomer();
        customer.setCurrency("USD");

        assertEquals(List.of(Messages.get("check.currency.vat")),
                InputCheck.errors(TestData.company(), TestData.invoice(customer)));
    }

    @Test
    void invoicesWithoutVatMayUseAnotherCurrency() {
        Customer customer = TestData.customer(TaxCase.THIRD_COUNTRY, "US", "");
        customer.setCurrency("usd");

        assertEquals(List.of(), InputCheck.errors(TestData.company(), TestData.invoice(customer)));
    }

    @Test
    void unknownCurrencyIsRejected() {
        Customer customer = TestData.customer(TaxCase.THIRD_COUNTRY, "US", "");
        customer.setCurrency("DOLLAR");

        assertEquals(List.of(Messages.get("check.customer.currency")),
                InputCheck.errors(TestData.company(), TestData.invoice(customer)));
    }

    @Test
    void reportsMissingHeaderDetails() {
        InvoiceData invoice = TestData.invoice(TestData.domesticCustomer());
        invoice.setNumber(" ");
        invoice.setDate(null);
        invoice.setServiceFrom(null);

        assertEquals(List.of(Messages.get("check.number"), Messages.get("check.date"), Messages.get("check.period")),
                InputCheck.errors(TestData.company(), invoice));
    }

    @Test
    void invoiceNumberMustBeUsableAsFileName() {
        for (String number : List.of("RE/2026/001", "..\\RE-1", "RE:2026", "RE-2026-1.", "RE..1", "RE?1")) {
            InvoiceData invoice = TestData.invoice(TestData.domesticCustomer());
            invoice.setNumber(number);

            assertEquals(List.of(Messages.get("check.numberCharacters")),
                    InputCheck.errors(TestData.company(), invoice), number);
        }
    }

    @Test
    void usualInvoiceNumbersAreAccepted() {
        for (String number : List.of("RE-2026-00043", "2026_17", "R 2026.11-3", "INV#12")) {
            InvoiceData invoice = TestData.invoice(TestData.domesticCustomer());
            invoice.setNumber(number);

            assertEquals(List.of(), InputCheck.errors(TestData.company(), invoice), number);
        }
    }

    @Test
    void reportsServicePeriodThatEndsBeforeItStarts() {
        InvoiceData invoice = TestData.invoice(TestData.domesticCustomer());
        invoice.setServiceTo(invoice.getServiceFrom().minusDays(1));

        assertEquals(List.of(Messages.get("check.periodOrder")), InputCheck.errors(TestData.company(), invoice));
    }

    @Test
    void reportsProblemsPerLineItem() {
        InvoiceData invoice = TestData.invoice(TestData.domesticCustomer());
        invoice.getItems().add(new LineItem(BigDecimal.ZERO, "h", "", null));

        List<String> errors = InputCheck.errors(TestData.company(), invoice);

        assertEquals(3, errors.size());
        assertTrue(errors.stream().allMatch(error -> error.startsWith("Position 2:")), errors.toString());
    }

    @Test
    void reportsInvoiceWithoutLineItems() {
        InvoiceData invoice = TestData.invoice(TestData.domesticCustomer());
        invoice.getItems().clear();

        assertEquals(List.of(Messages.get("check.noItems")), InputCheck.errors(TestData.company(), invoice));
    }
}
