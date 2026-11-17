package einvoice.ui;

import einvoice.core.EmailText;
import einvoice.i18n.Messages;
import einvoice.model.Company;
import einvoice.model.InvoiceData;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import javafx.animation.PauseTransition;
import javafx.scene.control.Button;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.util.Duration;

/**
 * Buttons that copy the subject and the text of the e-mail for an invoice to the clipboard, so the user can
 * paste them into their own mail program and attach the PDF.
 */
final class EmailCopyButtons {
    private static final Duration FEEDBACK_TIME = Duration.seconds(1.5);

    private EmailCopyButtons() {
    }

    /** Creates both buttons; company and invoice are read at the moment a button is pressed. */
    static List<Button> create(Supplier<Company> company, Supplier<InvoiceData> invoice) {
        return List.of(
                button("action.copySubject", EmailText::subject, company, invoice),
                button("action.copyEmailText", EmailText::body, company, invoice));
    }

    private static Button button(String labelKey, BiFunction<Company, InvoiceData, String> text,
                                 Supplier<Company> company, Supplier<InvoiceData> invoice) {
        Button button = new Button(Messages.get(labelKey));
        PauseTransition feedback = new PauseTransition(FEEDBACK_TIME);
        feedback.setOnFinished(event -> button.setText(Messages.get(labelKey)));
        button.setOnAction(event -> {
            if (invoice.get() == null || invoice.get().getCustomer() == null) {
                return;
            }
            ClipboardContent content = new ClipboardContent();
            content.putString(text.apply(company.get(), invoice.get()));
            Clipboard.getSystemClipboard().setContent(content);
            button.setText(Messages.get("action.copied"));
            feedback.playFromStart();
        });
        return button;
    }
}
