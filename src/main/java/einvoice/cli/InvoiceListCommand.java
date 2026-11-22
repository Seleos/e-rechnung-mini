package einvoice.cli;

import einvoice.core.Numbers;
import einvoice.i18n.Messages;
import einvoice.model.Company;
import einvoice.model.InvoiceData;

import java.util.Optional;
import java.util.Set;

/** {@code invoice list}: lists the invoices created with this app, newest number first, to pick a template. */
final class InvoiceListCommand implements Command {
    private static final String YEAR = "year";
    private static final String CUSTOMER = "customer";

    @Override
    public Set<String> options() {
        return Set.of(CommandContext.COMPANY, YEAR, CUSTOMER);
    }

    @Override
    public void run(Arguments arguments, CommandContext context) throws CommandFailure {
        Company company = context.company(arguments);
        Optional<Integer> year = year(arguments);
        Optional<String> customer = arguments.value(CUSTOMER);
        Table table = new Table().row(Messages.get("cli.column.number"), Messages.get("cli.column.date"),
                Messages.get("cli.column.customer"), Messages.get("cli.column.net"),
                Messages.get("cli.column.gross"));
        int shown = 0;
        for (InvoiceData invoice : context.store().records(company)) {
            if (matches(invoice, year, customer)) {
                String currency = invoice.getCustomer().getCurrency();
                table.row(invoice.getNumber(), CommandContext.format(invoice.getDate()),
                        invoice.getCustomer().getName(), Numbers.money(invoice.getNet(), currency),
                        Numbers.money(invoice.getGross(), currency));
                shown++;
            }
        }
        if (shown == 0) {
            context.out().println(Messages.get("cli.list.empty"));
            return;
        }
        table.print(context.out());
    }

    private static Optional<Integer> year(Arguments arguments) throws CommandFailure {
        Optional<String> value = arguments.value(YEAR);
        if (value.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(value.get().trim()));
        } catch (NumberFormatException e) {
            throw CommandFailure.usage(Messages.get("cli.error.year", value.get()));
        }
    }

    private static boolean matches(InvoiceData invoice, Optional<Integer> year, Optional<String> customer) {
        if (invoice.getCustomer() == null) {
            return false;
        }
        boolean yearMatches = year.isEmpty() || invoice.getDate().getYear() == year.get();
        return yearMatches && (customer.isEmpty() || customer.get().equals(invoice.getCustomer().getId()));
    }
}
