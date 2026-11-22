package einvoice.cli;

import einvoice.core.Numbers;
import einvoice.i18n.Messages;
import einvoice.model.Company;
import einvoice.model.Customer;
import einvoice.model.Service;

import java.util.List;
import java.util.Set;

/** {@code customer list}: lists the customers of a company with their tax case and agreed services. */
final class CustomerListCommand implements Command {
    @Override
    public Set<String> options() {
        return Set.of(CommandContext.COMPANY);
    }

    @Override
    public void run(Arguments arguments, CommandContext context) throws CommandFailure {
        Company company = context.company(arguments);
        Table table = new Table().row(Messages.get("cli.column.id"), Messages.get("cli.column.name"),
                Messages.get("cli.column.taxCase"), Messages.get("cli.column.services"));
        for (Customer customer : context.store().customers(company)) {
            List<Service> services = customer.getServices();
            String first = services.isEmpty() ? "" : describe(services.get(0), customer.getCurrency());
            table.row(customer.getId(), customer.getName(), customer.getTaxCase().label(), first);
            // Further services go on lines of their own, so each one stays readable.
            for (Service service : services.subList(Math.min(1, services.size()), services.size())) {
                table.row("", "", "", describe(service, customer.getCurrency()));
            }
        }
        table.print(context.out());
    }

    private static String describe(Service service, String currency) {
        String name = service.getText().lines().findFirst().orElse("");
        String price = Numbers.money(service.getUnitPrice(), currency) + "/" + service.getUnit();
        String text = Messages.get("cli.customers.service", name, price);
        return service.isIncludedByDefault() ? Messages.get("cli.customers.preFilled", text) : text;
    }
}
