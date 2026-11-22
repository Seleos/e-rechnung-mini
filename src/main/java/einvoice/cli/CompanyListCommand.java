package einvoice.cli;

import einvoice.i18n.Messages;
import einvoice.model.Company;

import java.util.Set;

/** {@code company list}: lists the companies of the data folder with the id that {@code --company} expects. */
final class CompanyListCommand implements Command {
    @Override
    public Set<String> options() {
        return Set.of();
    }

    @Override
    public void run(Arguments arguments, CommandContext context) {
        Table table = new Table().row(Messages.get("cli.column.id"), Messages.get("cli.column.name"));
        for (Company company : context.store().companies()) {
            table.row(company.getId(), company.getName());
        }
        table.print(context.out());
    }
}
