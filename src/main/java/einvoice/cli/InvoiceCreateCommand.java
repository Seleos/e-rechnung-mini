package einvoice.cli;

import einvoice.core.InputCheck;
import einvoice.i18n.Messages;
import einvoice.model.Company;
import einvoice.model.Customer;
import einvoice.model.InvoiceData;
import einvoice.service.InvoiceDrafts;
import einvoice.service.InvoiceService;
import einvoice.service.InvoiceService.InvalidInvoiceException;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * {@code invoice create}: writes a new invoice, either from an invoice created earlier ({@code --from}) or from the
 * pre-filled services of a customer ({@code --customer}). Number, date and service period are proposed as in
 * the user interface and can be changed with options.
 */
final class InvoiceCreateCommand implements Command {
    /** Flag that only shows the invoice without saving it. */
    static final String DRY_RUN = "dry-run";

    private static final String FROM = "from";
    private static final String CUSTOMER = "customer";
    private static final String DATE = "date";
    private static final String PERIOD = "period";
    private static final String SERVICE_FROM = "service-from";
    private static final String SERVICE_TO = "service-to";

    @Override
    public Set<String> options() {
        Set<String> options = new HashSet<>(Set.of(CommandContext.COMPANY, FROM, CUSTOMER, DATE, PERIOD,
                SERVICE_FROM, SERVICE_TO, DRY_RUN));
        options.addAll(LineItemOptions.NAMES);
        return options;
    }

    @Override
    public void run(Arguments arguments, CommandContext context) throws CommandFailure {
        Company company = context.company(arguments);
        LocalDate date = context.date(arguments, DATE).orElse(context.today());
        InvoiceData invoice = draft(arguments, context, company, date);
        applyServicePeriod(arguments, context, invoice);
        invoice.setItems(LineItemOptions.apply(arguments, invoice.getItems()));
        List<String> errors = InputCheck.errors(company, invoice);
        if (!errors.isEmpty()) {
            throw CommandFailure.failure(Messages.get("cli.create.incomplete", invoice.getNumber()), errors);
        }
        DraftReport.print(company, invoice, context.out());
        context.out().println();
        if (arguments.has(DRY_RUN)) {
            context.out().println(Messages.get("cli.create.dryRun"));
            return;
        }
        Path file = create(context, company, invoice);
        context.out().println(Messages.get("cli.create.done", invoice.getNumber(), file.toAbsolutePath()));
    }

    private static InvoiceData draft(Arguments arguments, CommandContext context, Company company, LocalDate date)
            throws CommandFailure {
        Optional<String> template = arguments.value(FROM);
        Optional<String> customerId = arguments.value(CUSTOMER);
        if (template.isPresent() == customerId.isPresent()) {
            throw CommandFailure.usage(Messages.get("cli.error.fromOrCustomer"));
        }
        InvoiceDrafts drafts = new InvoiceDrafts(context.store());
        if (template.isPresent()) {
            InvoiceData record = context.store().record(company, template.get())
                    .orElseThrow(() -> CommandFailure.usage(
                            Messages.get("cli.error.unknownInvoice", template.get(), company.getId())));
            return drafts.fromTemplate(company, record, date);
        }
        Customer customer = context.customer(company, customerId.get());
        return drafts.forCustomer(company, customer, date);
    }

    private static void applyServicePeriod(Arguments arguments, CommandContext context, InvoiceData invoice)
            throws CommandFailure {
        Optional<String> month = arguments.value(PERIOD);
        Optional<LocalDate> from = context.date(arguments, SERVICE_FROM);
        Optional<LocalDate> to = context.date(arguments, SERVICE_TO);
        if (month.isPresent()) {
            if (from.isPresent() || to.isPresent()) {
                throw CommandFailure.usage(Messages.get("cli.error.periodAndDates"));
            }
            YearMonth period = month(month.get());
            invoice.setServiceFrom(period.atDay(1));
            invoice.setServiceTo(period.atEndOfMonth());
        }
        from.ifPresent(invoice::setServiceFrom);
        to.ifPresent(invoice::setServiceTo);
    }

    private static YearMonth month(String value) throws CommandFailure {
        try {
            return YearMonth.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw CommandFailure.usage(Messages.get("cli.error.period", value));
        }
    }

    private static Path create(CommandContext context, Company company, InvoiceData invoice) throws CommandFailure {
        try {
            return new InvoiceService(context.store()).create(company, invoice);
        } catch (InvalidInvoiceException e) {
            throw CommandFailure.failure(Messages.get("cli.create.invalid", invoice.getNumber()),
                    e.result().problems());
        } catch (FileAlreadyExistsException e) {
            throw CommandFailure.failure(Messages.get("cli.create.exists", invoice.getNumber()), List.of());
        } catch (Exception e) {
            // Generation reports many unrelated library exceptions; the user needs the message, not the type.
            throw CommandFailure.failure(Messages.get("cli.create.failed", invoice.getNumber(), e.getMessage()),
                    List.of());
        }
    }
}
