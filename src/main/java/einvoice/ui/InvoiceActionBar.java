package einvoice.ui;

import einvoice.i18n.Messages;
import einvoice.model.InvoiceData;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;

/**
 * One row of buttons for a created invoice: open the PDF, show it in its folder and copy the e-mail
 * subject and text. Used after creating an invoice and below the invoice list, so both look the same.
 */
final class InvoiceActionBar extends FlowPane {
    private static final double GAP = 8;

    private final AppContext context;
    private final Supplier<InvoiceData> invoice;

    /** Creates the bar; the invoice is read at the moment a button is pressed. */
    InvoiceActionBar(AppContext context, Supplier<InvoiceData> invoice) {
        super(GAP, GAP);
        this.context = context;
        this.invoice = invoice;

        Button open = new Button(Messages.get("action.openPdf"));
        open.setOnAction(event -> openFile(false));
        Button showFolder = new Button(Messages.get("action.showInFolder"));
        showFolder.setOnAction(event -> openFile(true));
        getChildren().addAll(open, showFolder);
        getChildren().addAll(EmailCopyButtons.create(context::company, invoice));
    }

    /** Appends a button that only one of the pages needs. */
    void add(Button button) {
        getChildren().add(button);
    }

    /** Opens the PDF of the invoice, as the button does; does nothing for an invoice without a file. */
    void openPdf() {
        openFile(false);
    }

    /** Greys all buttons out while the condition holds, for example while no invoice is selected. */
    void disableWhile(ObservableValue<Boolean> condition) {
        getChildren().forEach(button -> button.disableProperty().bind(condition));
    }

    private void openFile(boolean folderOnly) {
        InvoiceData record = invoice.get();
        if (record == null || record.getFile() == null) {
            return;
        }
        Path file = context.invoiceService().file(context.company(), record);
        if (!Files.exists(file)) {
            Dialogs.error(Messages.get("invoices.fileMissing"), file.toString());
            return;
        }
        if (folderOnly) {
            context.showInFolder(file);
        } else {
            context.open(file);
        }
    }
}
