package einvoice.ui;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import einvoice.i18n.Language;
import einvoice.i18n.Messages;
import einvoice.storage.AppSettings;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Development tool: opens the app on a temporary data folder with sample data, creates one invoice through
 * the user interface, switches the language and writes a screenshot of every step to
 * {@code target/ui-snapshots}. The window takes the keyboard focus while it runs, so do not type during
 * a run: the keys would end up in the window and change the screenshots.
 * Used to review the user interface without clicking through it.
 */
public final class UiSnapshotTool extends Application {
    private static final Path OUTPUT = Path.of("target", "ui-snapshots");
    /** Set this system property to "true" to take the screenshots with the dark theme. */
    private static final String DARK_PROPERTY = "einvoice.snapshotDark";
    private static final Duration SETTLE_TIME = Duration.seconds(4);
    private static final Duration CREATE_TIME = Duration.seconds(12);

    private Scene scene;

    /**
     * One step of the walk-through.
     *
     * @param name     file name of the screenshot
     * @param action   what to do before the screenshot
     * @param waitTime time the user interface needs to settle afterwards
     */
    private record Step(String name, Runnable action, Duration waitTime) {
    }

    /**
     * Starts the tool.
     *
     * @param args not used
     * @throws IOException if the temporary data folder cannot be created
     */
    public static void main(String[] args) throws IOException {
        System.setProperty("java.awt.headless", "true");
        System.setProperty(AppSettings.DATA_DIR_PROPERTY, Files.createTempDirectory("einvoice-snapshot").toString());
        Files.createDirectories(OUTPUT);
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        boolean dark = Boolean.getBoolean(DARK_PROPERTY);
        Application.setUserAgentStylesheet(
                dark ? new PrimerDark().getUserAgentStylesheet() : new PrimerLight().getUserAgentStylesheet());
        AppContext context = new AppContext(new AppSettings(), getHostServices());
        scene = new Scene(new MainWindow(context), 1440, 900);
        context.onLanguageChange(() -> Platform.runLater(() -> App.rebuildWindow(scene, context)));
        Language otherLanguage = Messages.language() == Language.GERMAN ? Language.ENGLISH : Language.GERMAN;
        scene.getStylesheets().add(App.class.getResource("/einvoice/app.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
        runNext(new ArrayDeque<>(List.of(
                new Step("1-editor", this::fillInvoice, SETTLE_TIME),
                new Step("2-created", this::createInvoice, CREATE_TIME),
                new Step("3-invoices", () -> window().show(MainWindow.PageId.INVOICES), SETTLE_TIME),
                new Step("4-customers", () -> window().show(MainWindow.PageId.CUSTOMERS), SETTLE_TIME),
                new Step("5-settings", () -> window().show(MainWindow.PageId.SETTINGS), SETTLE_TIME),
                new Step("6-language-switched", () -> context.changeLanguage(otherLanguage), SETTLE_TIME))));
    }

    /** The window is replaced on a language change, so it is always read from the scene. */
    private MainWindow window() {
        return (MainWindow) scene.getRoot();
    }

    private void fillInvoice() {
        if (scene.lookup("." + LineItemRow.QUANTITY_STYLE_CLASS) instanceof TextField quantity) {
            quantity.setText("6,75");
        }
    }

    private void createInvoice() {
        if (scene.lookup("#" + InvoiceEditorPage.CREATE_BUTTON_ID) instanceof Button create) {
            create.fire();
        }
    }

    private void runNext(Deque<Step> remaining) {
        if (remaining.isEmpty()) {
            Platform.exit();
            return;
        }
        Step step = remaining.poll();
        step.action().run();
        PauseTransition wait = new PauseTransition(step.waitTime());
        wait.setOnFinished(event -> {
            writeScreenshot(step.name());
            runNext(remaining);
        });
        wait.play();
    }

    private void writeScreenshot(String name) {
        try {
            ImageIO.write(SwingFXUtils.fromFXImage(scene.snapshot(null), null), "png",
                    OUTPUT.resolve(name + ".png").toFile());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
