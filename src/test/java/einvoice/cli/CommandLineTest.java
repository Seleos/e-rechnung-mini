package einvoice.cli;

import einvoice.AppInfo;
import einvoice.TestData;
import einvoice.i18n.Language;
import einvoice.i18n.Messages;
import einvoice.model.Company;
import einvoice.model.Customer;
import einvoice.model.InvoiceData;
import einvoice.model.Service;
import einvoice.model.TaxCase;
import einvoice.storage.DataStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Runs the command line against a data folder, including invoice generation with validation. */
class CommandLineTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 22);
    private static final String CUSTOMER = "beispielkunde-gmbh";

    @TempDir
    private Path dataDir;
    private DataStore store;
    private Company company;
    private Customer customer;
    private Language previousLanguage;
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    @BeforeEach
    void setUp() {
        previousLanguage = Messages.language();
        Messages.setLanguage(Language.GERMAN);
        store = new DataStore(dataDir);
        company = TestData.company();
        store.save(company);
        customer = TestData.domesticCustomer();
        Service maintenance = new Service("Wartung\nLeistungszeitraum: {period}", "h", new BigDecimal("70.00"));
        maintenance.setIncludedByDefault(true);
        customer.getServices().add(maintenance);
        store.save(company, customer);
    }

    @AfterEach
    void restoreLanguage() {
        Messages.setLanguage(previousLanguage);
    }

    @Test
    void createFromCustomerWritesAValidatedInvoiceAndItsRecord() {
        assertEquals(CommandLine.SUCCESS, run("invoice", "create", "--customer", CUSTOMER, "--qty", "6,75"));

        InvoiceData record = store.record(company, "RE-2026-00001").orElseThrow();
        assertTrue(Files.isRegularFile(store.outputDir(company, 2026).resolve("RE-2026-00001.pdf")));
        assertEquals(new BigDecimal("6.75"), record.getItems().get(0).getQuantity());
        assertEquals(LocalDate.of(2026, 10, 1), record.getServiceFrom());
        assertEquals(new BigDecimal("562.28"), record.getGross());
        assertTrue(output().contains("Leistungszeitraum: Oktober 2026"), output());
        assertTrue(output().contains("RE-2026-00001.pdf"), output());
    }

    @Test
    void createFromTemplateTakesCustomerAndLinesAndTheNextNumber() {
        run("invoice", "create", "--customer", CUSTOMER, "--qty", "6,75");
        customer.setStreet("Neue Straße 5");
        store.save(company, customer);

        int exitCode = run("invoice", "create", "--from", "RE-2026-00001", "--price", "80", "--period", "2026-11",
                "--date", "2026-12-01");

        assertEquals(CommandLine.SUCCESS, exitCode, errors());
        InvoiceData record = store.record(company, "RE-2026-00002").orElseThrow();
        assertEquals(new BigDecimal("6.75"), record.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("80"), record.getItems().get(0).getUnitPrice());
        assertEquals(LocalDate.of(2026, 12, 1), record.getDate());
        assertEquals(LocalDate.of(2026, 11, 1), record.getServiceFrom());
        assertEquals(LocalDate.of(2026, 11, 30), record.getServiceTo());
        assertEquals("Neue Straße 5", record.getCustomer().getStreet());
    }

    @Test
    void dryRunShowsTheInvoiceButSavesNothing() {
        int exitCode = run("invoice", "create", "--customer", CUSTOMER, "--item", "Beratung;2;h;100",
                "--item", "Reisekosten;1;pauschal;50", "--service-from", "2026-11-03", "--service-to", "2026-11-04",
                "--dry-run");

        assertEquals(CommandLine.SUCCESS, exitCode, errors());
        assertTrue(store.records(company).isEmpty());
        assertTrue(output().contains("Reisekosten"), output());
        assertTrue(output().contains("297,50"), output());
        assertTrue(output().contains("03.11.2026"), output());
        assertTrue(output().contains(Messages.get("cli.create.dryRun")), output());
    }

    @Test
    void incompleteInvoiceIsRejectedWithItsProblems() {
        assertEquals(CommandLine.FAILURE, run("invoice", "create", "--customer", CUSTOMER));

        assertTrue(errors().contains(Messages.get("check.item.prefix", 1)), errors());
        assertTrue(store.records(company).isEmpty());
    }

    @Test
    void invoiceForAThirdCountryCustomerNeedsTheTaxNumber() {
        company.setTaxNumber("");
        store.save(company);
        Customer swiss = TestData.customer(TaxCase.THIRD_COUNTRY, "CH", "");
        swiss.setName("Kunde AG");
        store.save(company, swiss);

        assertEquals(CommandLine.FAILURE,
                run("invoice", "create", "--customer", "kunde-ag", "--item", "Beratung;1;h;100"));

        assertTrue(errors().contains(Messages.get("check.company.taxNumber")), errors());
    }

    static Stream<List<String>> wrongArguments() {
        return Stream.of(
                List.of("send"),
                List.of("send", "invoice"),
                List.of("invoice", "create", "--from", "RE-2026-00001", "--customer", CUSTOMER),
                List.of("invoice", "create"),
                List.of("invoice", "create", "--customer", "unbekannt"),
                List.of("invoice", "create", "--from", "RE-2026-00099"),
                List.of("invoice", "create", "--company", "unbekannt", "--customer", CUSTOMER),
                List.of("invoice", "create", "--customer", CUSTOMER, "--date", "22.11.2026"),
                List.of("invoice", "create", "--customer", CUSTOMER, "--period", "November"),
                List.of("invoice", "create", "--customer", CUSTOMER, "--period", "2026-11",
                        "--service-to", "2026-11-30"),
                List.of("invoice", "create", "--customer", CUSTOMER, "--qty", "1", "--qty", "2"),
                List.of("invoice", "create", "--customer", CUSTOMER, "--gty", "1"),
                List.of("invoice", "list", "--year", "zweitausend"),
                List.of("company", "list", "--company", "musterfirma-gmbh"),
                List.of("invoice"),
                List.of("invoice", "delete"),
                List.of("invoices", "list"));
    }

    @ParameterizedTest
    @MethodSource("wrongArguments")
    void wrongArgumentsEndWithUsageCodeAndHint(List<String> args) {
        assertEquals(CommandLine.USAGE, run(args.toArray(String[]::new)));

        assertTrue(errors().contains(Messages.get("cli.hint")), errors());
        assertTrue(store.records(company).isEmpty());
    }

    @Test
    void missingOrUnknownActionNamesTheActionsOfTheEntity() {
        assertEquals(CommandLine.USAGE, run("invoice", "delete"));

        assertTrue(errors().contains(Messages.get("cli.error.unknownAction", "invoice", "create, list")), errors());
    }

    @Test
    void companyMustBeChosenOnlyWhenThereAreSeveral() {
        assertEquals(CommandLine.SUCCESS, run("customer", "list"));
        store.save(TestData.company());

        assertEquals(CommandLine.USAGE, run("customer", "list"));
        assertTrue(errors().contains("musterfirma-gmbh-2"), errors());
        assertEquals(CommandLine.SUCCESS, run("customer", "list", "--company", "musterfirma-gmbh"));
    }

    @Test
    void emptyDataFolderIsReported(@TempDir Path emptyDir) {
        assertEquals(CommandLine.FAILURE, run("invoice", "list", "--data-dir", emptyDir.toString()));

        assertTrue(errors().contains(emptyDir.toString()), errors());
    }

    @Test
    void listShowsCreatedInvoicesFilteredByYearAndCustomer() {
        run("invoice", "create", "--customer", CUSTOMER, "--qty", "1");
        out.reset();

        assertEquals(CommandLine.SUCCESS, run("invoice", "list", "--year", "2026", "--customer", CUSTOMER));
        assertTrue(output().contains("RE-2026-00001"), output());
        assertTrue(output().contains("83,30"), output());

        out.reset();
        assertEquals(CommandLine.SUCCESS, run("invoice", "list", "--year", "2025"));
        assertEquals(Messages.get("cli.list.empty"), output().strip());

        out.reset();
        assertEquals(CommandLine.SUCCESS, run("invoice", "list", "--customer", "andere"));
        assertFalse(output().contains("RE-2026-00001"), output());
    }

    @Test
    void companiesAndCustomersShowTheIdsTheOptionsExpect() {
        customer.getServices().add(new Service("Entwicklung", "h", new BigDecimal("95.00")));
        store.save(company, customer);

        assertEquals(CommandLine.SUCCESS, run("company", "list"));
        assertEquals(CommandLine.SUCCESS, run("customer", "list"));

        assertTrue(output().contains("musterfirma-gmbh  Musterfirma GmbH"), output());
        assertTrue(output().contains(CUSTOMER), output());
        assertTrue(output().contains("Wartung (70,00 €/h), vorbelegt"), output());
        assertTrue(output().contains("Entwicklung (95,00 €/h)" + System.lineSeparator()), output());
    }

    @Test
    void helpAndVersionNeedNoData(@TempDir Path emptyDir) {
        assertEquals(CommandLine.SUCCESS, run("--help"));
        assertEquals(CommandLine.SUCCESS, run("invoice", "create", "--from", "RE-1", "--help"));
        assertEquals(CommandLine.SUCCESS, run("invoice", "--help"));
        assertEquals(CommandLine.SUCCESS, run("--version", "--data-dir", emptyDir.toString()));

        assertTrue(output().contains("--dry-run"), output());
        assertTrue(output().contains("{period}"), output());
        assertTrue(output().contains("pauschal"), output());
        assertTrue(output().contains(AppInfo.version()), output());
    }

    @Test
    void damagedFilesAreSkippedWithAWarning() throws IOException {
        Files.writeString(store.companyDir(company).resolve("customers").resolve("kaputt.json"), "{");

        assertEquals(CommandLine.SUCCESS, run("customer", "list"));

        assertTrue(errors().contains("kaputt.json"), errors());
        assertTrue(output().contains(CUSTOMER), output());
    }

    @Test
    void wordsGoToTheCommandLineAndSystemOptionsToTheWindow() {
        assertTrue(CommandLine.isCommand(new String[] {"invoice", "create"}));
        assertTrue(CommandLine.isCommand(new String[] {"customer"}));
        assertTrue(CommandLine.isCommand(new String[] {"help"}));
        assertTrue(CommandLine.isCommand(new String[] {"--help"}));
        assertTrue(CommandLine.isCommand(new String[] {"-h"}));
        assertTrue(CommandLine.isCommand(new String[] {"--version"}));
        assertFalse(CommandLine.isCommand(new String[] {}));
        assertTrue(CommandLine.isCommand(new String[] {"customers"}));
        assertFalse(CommandLine.isCommand(new String[] {"-psn_0_12345"}));
        assertFalse(CommandLine.isCommand(new String[] {"--unknown"}));
    }

    private int run(String... args) {
        PrintStream output = new PrintStream(out, true, StandardCharsets.UTF_8);
        PrintStream errorOutput = new PrintStream(err, true, StandardCharsets.UTF_8);
        Clock clock = Clock.fixed(TODAY.atStartOfDay(ZoneId.of("Europe/Berlin")).toInstant(),
                ZoneId.of("Europe/Berlin"));
        return new CommandLine(dataDir, clock, output, errorOutput).run(args);
    }

    private String output() {
        return out.toString(StandardCharsets.UTF_8);
    }

    private String errors() {
        return err.toString(StandardCharsets.UTF_8);
    }
}
