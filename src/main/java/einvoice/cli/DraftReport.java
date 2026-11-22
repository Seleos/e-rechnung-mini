package einvoice.cli;

import einvoice.core.Numbers;
import einvoice.core.Placeholders;
import einvoice.core.XmlBuilder;
import einvoice.i18n.Messages;
import einvoice.model.Company;
import einvoice.model.InvoiceData;
import einvoice.model.LineItem;

import java.io.PrintStream;

/** Prints an invoice before or after it is created, so the user can check what the file contains. */
final class DraftReport {
    private static final String LINE_SEPARATOR = " / ";

    private DraftReport() {
    }

    /**
     * Prints number, customer, service period, line items and totals.
     *
     * @param company issuing company
     * @param invoice invoice that passed the input check
     * @param out     target of the output
     */
    static void print(Company company, InvoiceData invoice, PrintStream out) {
        String currency = invoice.getCustomer().getCurrency();
        out.println(Messages.get("cli.report.invoice", invoice.getNumber(), CommandContext.format(invoice.getDate())));
        out.println(Messages.get("cli.report.customer", invoice.getCustomer().getName(),
                invoice.getCustomer().getTaxCase().label()));
        out.println(Messages.get("cli.report.period", CommandContext.format(invoice.getServiceFrom()),
                CommandContext.format(invoice.getServiceTo())));
        out.println();
        Table lines = new Table().row(Messages.get("cli.column.position"), Messages.get("cli.column.quantity"),
                Messages.get("cli.column.text"), Messages.get("cli.column.unitPrice"));
        int position = 1;
        for (LineItem item : invoice.getItems()) {
            // Shown with placeholders replaced, as they will appear on the invoice.
            String text = Placeholders.fill(item.getText(), company, invoice).replace("\n", LINE_SEPARATOR);
            lines.row(String.valueOf(position++), Numbers.quantity(item.getQuantity()) + " " + item.getUnit(), text,
                    Numbers.money(item.getUnitPrice(), currency));
        }
        lines.print(out);
        out.println();
        XmlBuilder.Totals totals = XmlBuilder.totals(company, invoice);
        new Table()
                .row(Messages.get("cli.report.net"), Numbers.money(totals.net(), currency))
                .row(Messages.get("cli.report.tax"), Numbers.money(totals.tax(), currency))
                .row(Messages.get("cli.report.gross"), Numbers.money(totals.gross(), currency))
                .print(out);
    }
}
