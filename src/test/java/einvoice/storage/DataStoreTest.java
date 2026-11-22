package einvoice.storage;

import einvoice.TestData;
import einvoice.model.Company;
import einvoice.model.Customer;
import einvoice.model.InvoiceData;
import einvoice.model.Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests reading and writing the data folder and the proposal of invoice numbers. */
class DataStoreTest {
    private static final LocalDate INVOICE_DATE = LocalDate.of(2026, 11, 17);

    private Path root;
    private DataStore store;
    private Company company;

    @BeforeEach
    void setUp(@TempDir Path dataDir) {
        root = dataDir;
        store = new DataStore(dataDir);
        company = TestData.company();
        store.save(company);
    }

    @Test
    void companyGetsReadableIdAndIsLoadedAgain() {
        assertEquals("musterfirma-gmbh", company.getId());
        assertTrue(Files.isRegularFile(root.resolve("companies/musterfirma-gmbh/company.json")));

        List<Company> loaded = new DataStore(root).companies();

        assertEquals(1, loaded.size());
        assertEquals("Musterfirma GmbH", loaded.get(0).getName());
        assertEquals("DE36 0000 0000 0000 0000 00", loaded.get(0).getIban());
    }

    @Test
    void idsAreUniqueAndFreeOfSpecialCharacters() {
        Company second = TestData.company();
        store.save(second);
        Company umlauts = TestData.company();
        umlauts.setName("Müller & Söhne – Bürobedarf");
        store.save(umlauts);

        assertEquals("musterfirma-gmbh-2", second.getId());
        assertEquals("mueller-soehne-buerobedarf", umlauts.getId());
        assertEquals(3, store.companies().size());
    }

    @Test
    void customerIsStoredWithItsServicesAndRates() {
        Customer customer = TestData.domesticCustomer();
        customer.getServices().add(new Service("Wartung", "h", new BigDecimal("70.00")));
        customer.getServices().add(new Service("Entwicklung", "h", new BigDecimal("95.00")));
        customer.getServices().get(0).setIncludedByDefault(true);
        store.save(company, customer);

        List<Customer> loaded = store.customers(company);

        assertEquals(1, loaded.size());
        assertEquals("beispielkunde-gmbh", loaded.get(0).getId());
        assertEquals(2, loaded.get(0).getServices().size());
        assertEquals(new BigDecimal("95.00"), loaded.get(0).getServices().get(1).getUnitPrice());
        assertTrue(loaded.get(0).getServices().get(0).isIncludedByDefault());
        assertFalse(loaded.get(0).getServices().get(1).isIncludedByDefault());
    }

    @Test
    void companiesAndCustomersAreSortedByName() {
        for (String name : List.of("Zeta AG", "alpha GmbH", "Mitte KG")) {
            Company other = TestData.company();
            other.setName(name);
            store.save(other);
            Customer customer = TestData.domesticCustomer();
            customer.setName(name);
            store.save(company, customer);
        }

        assertEquals(List.of("alpha GmbH", "Mitte KG", "Musterfirma GmbH", "Zeta AG"),
                store.companies().stream().map(Company::getName).toList());
        assertEquals(List.of("alpha GmbH", "Mitte KG", "Zeta AG"),
                store.customers(company).stream().map(Customer::getName).toList());
    }

    @Test
    void savingACustomerAgainKeepsItsIdAndFile() {
        Customer customer = TestData.domesticCustomer();
        store.save(company, customer);
        customer.setCity("Berlin");
        store.save(company, customer);
        Customer namesake = TestData.domesticCustomer();
        store.save(company, namesake);
        Customer third = TestData.domesticCustomer();
        store.save(company, third);

        assertEquals("beispielkunde-gmbh", customer.getId());
        assertEquals("beispielkunde-gmbh-2", namesake.getId());
        assertEquals("beispielkunde-gmbh-3", third.getId());
        assertEquals(3, store.customers(company).size());
    }

    @Test
    void foldersAndOtherFilesBetweenTheDataAreIgnored() throws IOException {
        store.save(company, TestData.domesticCustomer());
        Files.createDirectories(store.companyDir(company).resolve("customers").resolve("archiv.json"));
        Files.writeString(root.resolve("companies").resolve("notiz.txt"), "kein Firmenordner");

        assertEquals(1, store.customers(company).size());
        assertEquals(1, store.companies().size());
        assertEquals(List.of(), store.takeUnreadableFiles());
    }

    @Test
    void newLogoReplacesTheOldFileAndIsRemembered() throws IOException {
        store.setLogo(company, Files.writeString(root.resolve("first.png"), "image"));
        Path first = store.logoFile(company);
        store.setLogo(company, Files.writeString(root.resolve("second.svg"), "<svg/>"));

        assertFalse(Files.exists(first), "the old logo file is deleted");
        assertEquals("logo.svg", new DataStore(root).companies().get(0).getLogo(), "the new logo is saved");

        Path second = store.logoFile(company);
        store.removeLogo(company);

        assertFalse(Files.exists(second), "removing the logo deletes its file");
    }

    @Test
    void deletedCustomerIsGone() throws IOException {
        Customer customer = TestData.domesticCustomer();
        store.save(company, customer);

        store.delete(company, customer);

        assertEquals(List.of(), store.customers(company));
    }

    @Test
    void firstInvoiceNumberOfAYearEndsWithOne() {
        assertEquals("RE-2026-00001", store.nextNumber(company, INVOICE_DATE));
    }

    @Test
    void nextNumberFollowsTheHighestFileInTheOutputFolder() throws IOException {
        Path output = store.outputDir(company, 2026);
        Files.createDirectories(output);
        Files.createFile(output.resolve("RE-2026-00041.pdf"));
        Files.createFile(output.resolve("RE-2026-00042.docx"));
        Files.createFile(output.resolve("RE-2025-00099.pdf"));
        Files.createFile(output.resolve("Time-Report-2026-10.pdf"));

        assertEquals("RE-2026-00043", store.nextNumber(company, INVOICE_DATE));
    }

    @Test
    void nextNumberAlsoRespectsStoredRecords() {
        InvoiceData record = TestData.invoice(TestData.domesticCustomer());
        record.setNumber("RE-2026-00007");
        store.saveRecord(company, record);

        assertEquals("RE-2026-00008", store.nextNumber(company, INVOICE_DATE));
        assertEquals("RE-2027-00001", store.nextNumber(company, INVOICE_DATE.plusYears(1)));
    }

    @Test
    void veryLongDigitRunsInFileNamesAreIgnored() throws IOException {
        Path output = store.outputDir(company, 2026);
        Files.createDirectories(output);
        Files.createFile(output.resolve("RE-2026-20261117123456.pdf"));
        Files.createFile(output.resolve("RE-2026-00005.pdf"));

        assertEquals("RE-2026-00006", store.nextNumber(company, INVOICE_DATE));
    }

    @Test
    void existingRecordIsNeverReplaced() {
        InvoiceData record = TestData.invoice(TestData.domesticCustomer());
        store.saveRecord(company, record);

        assertTrue(store.hasRecord(company, record.getNumber()));
        assertThrows(UncheckedIOException.class, () -> store.saveRecord(company, record));
    }

    @Test
    void damagedFilesAreSkippedAndReportedOnce() throws IOException {
        store.save(company, TestData.domesticCustomer());
        Path damaged = store.companyDir(company).resolve("customers").resolve("kaputt.json");
        Files.writeString(damaged, "{ \"name\": \"Abgeschn");

        List<Customer> customers = store.customers(company);

        assertEquals(1, customers.size());
        assertEquals(List.of(damaged), store.takeUnreadableFiles());
        assertEquals(List.of(), store.takeUnreadableFiles());
    }

    @Test
    void damagedCompanyFileDoesNotHideTheOtherCompanies() throws IOException {
        Company second = TestData.company();
        second.setName("Zweitfirma UG");
        store.save(second);
        Files.writeString(store.companyDir(second).resolve("company.json"), "");

        assertEquals(1, store.companies().size());
        assertEquals(1, store.takeUnreadableFiles().size());
    }

    @Test
    void savingLeavesNoTemporaryFiles() throws IOException {
        store.save(company, TestData.domesticCustomer());

        try (var files = Files.walk(root)) {
            assertTrue(files.noneMatch(file -> file.toString().endsWith(".tmp")));
        }
    }

    @Test
    void numberFormatFollowsPrefixAndDigits() {
        company.setNumberPrefix("{year}/");
        company.setNumberDigits(3);

        assertEquals("2026/001", store.nextNumber(company, INVOICE_DATE));
    }

    @Test
    void recordsAreListedNewestNumberFirst() {
        for (String number : List.of("RE-2026-00001", "RE-2026-00003", "RE-2026-00002")) {
            InvoiceData record = TestData.invoice(TestData.domesticCustomer());
            record.setNumber(number);
            store.saveRecord(company, record);
        }

        List<String> numbers = store.records(company).stream().map(InvoiceData::getNumber).toList();

        assertEquals(List.of("RE-2026-00003", "RE-2026-00002", "RE-2026-00001"), numbers);
        assertEquals(LocalDate.of(2026, 10, 31), store.records(company).get(0).getServiceTo());
    }

    @Test
    void recordIsFoundByItsNumberOnly() {
        InvoiceData record = TestData.invoice(TestData.domesticCustomer());
        store.saveRecord(company, record);

        assertEquals("Beispielkunde GmbH",
                store.record(company, "RE-2026-00043").orElseThrow().getCustomer().getName());
        assertTrue(store.record(company, "RE-2026-00044").isEmpty());
        assertTrue(store.record(company, "../records/RE-2026-00043").isEmpty());
    }

    @Test
    void outputFolderDefaultsToTheCompanyFolderAndReplacesTheYear() {
        assertEquals(store.companyDir(company).resolve("invoices").resolve("2026"), store.outputDir(company, 2026));

        company.setOutputDir(root.resolve("out").resolve("{year} - Ausgang").toString());

        assertEquals(root.resolve("out").resolve("2026 - Ausgang"), store.outputDir(company, 2026));
    }

    @Test
    void logoIsCopiedIntoTheCompanyFolderAndCanBeRemoved() throws IOException {
        Path source = Files.writeString(root.resolve("my-logo.PNG"), "image");
        assertNull(store.logoFile(company));

        store.setLogo(company, source);

        assertEquals("logo.png", company.getLogo());
        assertNotNull(store.logoFile(company));

        store.removeLogo(company);

        assertNull(store.logoFile(company));
        assertEquals("", company.getLogo());
    }

    @Test
    void exportedTemplateIsUsedAndNotOverwritten() throws IOException {
        assertNull(store.templateFile(company));

        Path template = store.exportTemplate(company);
        Files.writeString(template, "adapted");
        store.exportTemplate(company);

        assertEquals(template, store.templateFile(company));
        assertEquals("adapted", Files.readString(template));
    }

    @Test
    void sampleDataIsCompleteEnoughToWriteAnInvoice() throws IOException {
        Company sample = SampleData.create(new DataStore(root.resolve("fresh")));
        DataStore fresh = new DataStore(root.resolve("fresh"));

        assertEquals(3, fresh.customers(sample).size());
        assertTrue(fresh.customers(sample).stream().noneMatch(customer -> customer.getServices().isEmpty()));
        assertNotNull(fresh.logoFile(sample));
    }
}
