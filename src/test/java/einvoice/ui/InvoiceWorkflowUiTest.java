package einvoice.ui;

import einvoice.model.Customer;
import einvoice.model.InvoiceData;
import einvoice.storage.AppSettings;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the real user interface: opens the app on a fresh data folder, presses "create invoice" and checks
 * that a validated file and its record exist. Skipped on machines without a display.
 */
class InvoiceWorkflowUiTest {
    private static final Duration CREATE_TIMEOUT = Duration.ofSeconds(90);

    @BeforeAll
    static void startJavaFx(@TempDir Path dataDir) {
        System.setProperty("java.awt.headless", "true");
        System.setProperty(AppSettings.DATA_DIR_PROPERTY, dataDir.toString());
        System.setProperty(AppSettings.LANGUAGE_PROPERTY, "de");
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyStarted) {
            // Another test class started the toolkit; nothing to do.
        } catch (UnsupportedOperationException | UnsatisfiedLinkError noDisplay) {
            Assumptions.abort("JavaFX cannot start without a display: " + noDisplay.getMessage());
        }
        Platform.setImplicitExit(false);
    }

    @Test
    void createsAValidatedInvoiceThroughTheUserInterface() throws Exception {
        AppContext context = onFxThread(() -> new AppContext(new AppSettings(), null));
        Scene scene = onFxThread(() -> new Scene(new MainWindow(context), 1440, 900));

        onFxThread(() -> {
            // The window is never shown, so the controls have to be built explicitly before they can be found.
            scene.getRoot().applyCss();
            scene.getRoot().layout();
            // A new invoice is pre-filled with the customer's default service; only the quantity is missing.
            ((TextField) scene.lookup("." + LineItemRow.QUANTITY_STYLE_CLASS)).setText("1");
            ((Button) scene.lookup("#" + InvoiceEditorPage.CREATE_BUTTON_ID)).fire();
            return null;
        });
        waitUntilVisible(scene.lookup("#" + InvoiceEditorPage.RESULT_BOX_ID));

        List<InvoiceData> records = context.store().records(context.company());
        assertEquals(1, records.size());
        InvoiceData record = records.get(0);
        Path pdf = context.invoiceService().file(context.company(), record);
        assertTrue(Files.size(pdf) > 0, "the PDF was written");
        assertEquals("Beispielkunde GmbH", record.getCustomer().getName());
        assertEquals("Wartung {month} {year}", record.getItems().get(0).getText(), "the record keeps the template");
        assertEquals(0, record.getGross().compareTo(new java.math.BigDecimal("83.30")), "1 h at 70 euro plus VAT");
        assertTrue(context.store().nextNumber(context.company(), record.getDate()).endsWith("00002"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void switchingTheCustomerReplacesUntouchedDefaultLines() throws Exception {
        AppContext context = onFxThread(() -> new AppContext(new AppSettings(), null));
        Scene scene = onFxThread(() -> new Scene(new MainWindow(context), 1440, 900));

        String textAfterSwitch = onFxThread(() -> {
            scene.getRoot().applyCss();
            scene.getRoot().layout();
            ComboBox<Customer> customer = (ComboBox<Customer>) scene.lookup("#" + InvoiceEditorPage.CUSTOMER_BOX_ID);
            assertEquals("Wartung {month} {year}", firstLineText(scene), "pre-filled service of the first customer");
            customer.setValue(customer.getItems().stream()
                    .filter(candidate -> candidate.getName().equals("Exemple SARL"))
                    .findFirst()
                    .orElseThrow());
            scene.getRoot().applyCss();
            scene.getRoot().layout();
            return firstLineText(scene);
        });

        assertEquals("Softwareentwicklung", textAfterSwitch, "pre-filled service of the newly selected customer");
    }

    @Test
    @SuppressWarnings("unchecked")
    void linesTheUserHasFilledInSurviveACustomerSwitch() throws Exception {
        AppContext context = onFxThread(() -> new AppContext(new AppSettings(), null));
        Scene scene = onFxThread(() -> new Scene(new MainWindow(context), 1440, 900));

        String textAfterSwitch = onFxThread(() -> {
            scene.getRoot().applyCss();
            scene.getRoot().layout();
            ((TextField) scene.lookup("." + LineItemRow.QUANTITY_STYLE_CLASS)).setText("3");
            ComboBox<Customer> customer = (ComboBox<Customer>) scene.lookup("#" + InvoiceEditorPage.CUSTOMER_BOX_ID);
            customer.setValue(customer.getItems().get(customer.getItems().size() - 1));
            return firstLineText(scene);
        });

        assertEquals("Wartung {month} {year}", textAfterSwitch);
    }

    private static String firstLineText(Scene scene) {
        return ((TextArea) scene.lookup(".text-area")).getText();
    }

    private static void waitUntilVisible(Node node) throws Exception {
        Instant deadline = Instant.now().plus(CREATE_TIMEOUT);
        while (!onFxThread(node::isVisible)) {
            assertTrue(Instant.now().isBefore(deadline), "the invoice was not created in time");
            TimeUnit.MILLISECONDS.sleep(200);
        }
    }

    private static <T> T onFxThread(Callable<T> action) throws Exception {
        CompletableFuture<T> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                result.complete(action.call());
            } catch (Exception e) {
                result.completeExceptionally(e);
            }
        });
        return result.get(CREATE_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
    }
}
