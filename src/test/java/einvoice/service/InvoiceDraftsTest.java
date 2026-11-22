package einvoice.service;

import einvoice.TestData;
import einvoice.model.Company;
import einvoice.model.Customer;
import einvoice.model.InvoiceData;
import einvoice.model.LineItem;
import einvoice.model.Service;
import einvoice.storage.DataStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the preparation of new invoices from a customer or from an earlier invoice. */
class InvoiceDraftsTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 22);

    private DataStore store;
    private InvoiceDrafts drafts;
    private Company company;
    private Customer customer;

    @BeforeEach
    void setUp(@TempDir Path dataDir) {
        store = new DataStore(dataDir);
        drafts = new InvoiceDrafts(store);
        company = TestData.company();
        store.save(company);
        customer = TestData.domesticCustomer();
        Service maintenance = new Service("Wartung {month}", "h", new BigDecimal("70.00"));
        maintenance.setIncludedByDefault(true);
        customer.getServices().add(maintenance);
        customer.getServices().add(new Service("Entwicklung", "h", new BigDecimal("95.00")));
        store.save(company, customer);
    }

    @Test
    void draftForCustomerHasNextNumberPreviousMonthAndPreFilledServicesWithoutQuantity() {
        InvoiceData draft = drafts.forCustomer(company, customer, TODAY);

        assertEquals("RE-2026-00001", draft.getNumber());
        assertEquals(TODAY, draft.getDate());
        assertEquals(LocalDate.of(2026, 10, 1), draft.getServiceFrom());
        assertEquals(LocalDate.of(2026, 10, 31), draft.getServiceTo());
        assertEquals(1, draft.getItems().size());
        assertEquals("Wartung {month}", draft.getItems().get(0).getText());
        assertNull(draft.getItems().get(0).getQuantity());
        assertNotSame(customer, draft.getCustomer());
    }

    @Test
    void draftFromTemplateCopiesLinesAndTakesTheFollowingNumber() throws IOException {
        InvoiceData template = TestData.invoice(customer);
        template.setNumber("RE-2026-00043");
        store.saveRecord(company, template);
        Files.createDirectories(store.outputDir(company, 2026));
        Files.createFile(store.outputDir(company, 2026).resolve("RE-2026-00043.pdf"));

        InvoiceData draft = drafts.fromTemplate(company, template, TODAY);
        draft.getItems().get(0).setQuantity(BigDecimal.ONE);

        assertEquals("RE-2026-00044", draft.getNumber());
        assertEquals(LocalDate.of(2026, 10, 1), draft.getServiceFrom());
        assertEquals(new BigDecimal("6.75"), template.getItems().get(0).getQuantity());
        LineItem copied = draft.getItems().get(0);
        assertEquals(template.getItems().get(0).getText(), copied.getText());
        assertEquals(new BigDecimal("70.00"), copied.getUnitPrice());
    }

    @Test
    void draftFromTemplateUsesTheCustomerAsStoredNow() {
        InvoiceData template = TestData.invoice(customer.copy());
        customer.setStreet("Neue Straße 5");
        store.save(company, customer);

        InvoiceData draft = drafts.fromTemplate(company, template, TODAY);

        assertEquals("Neue Straße 5", draft.getCustomer().getStreet());
    }

    @Test
    void draftFromTemplateFallsBackToTheCopyOfADeletedCustomer() throws IOException {
        InvoiceData template = TestData.invoice(customer.copy());
        store.delete(company, customer);

        InvoiceData draft = drafts.fromTemplate(company, template, TODAY);

        assertEquals("Kundenallee 12", draft.getCustomer().getStreet());
        assertNotSame(template.getCustomer(), draft.getCustomer());
    }

    @Test
    void draftFromTemplateWithoutCustomerHasNone() {
        InvoiceData template = TestData.invoice(null);

        assertNull(drafts.fromTemplate(company, template, TODAY).getCustomer());
    }

    @Test
    void defaultLinesContainOnlyPreFilledServices() {
        assertEquals(List.of("Wartung {month}"),
                InvoiceDrafts.defaultLines(customer).stream().map(LineItem::getText).toList());
        assertTrue(InvoiceDrafts.defaultLines(null).isEmpty());
    }

    @Test
    void defaultServiceMonthIsThePreviousMonthAcrossTheTurnOfTheYear() {
        assertEquals(YearMonth.of(2025, 12), InvoiceDrafts.defaultServiceMonth(LocalDate.of(2026, 1, 15)));
    }
}
