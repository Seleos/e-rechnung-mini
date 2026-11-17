package einvoice.ui;

import atlantafx.base.theme.Styles;
import einvoice.core.PdfBuilder;
import einvoice.i18n.Messages;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Shows the first page of the invoice PDF and re-renders it shortly after the last change. */
final class PreviewPane extends VBox {
    private static final float RENDER_DPI = 130;
    private static final Duration DELAY = Duration.millis(350);
    private static final double PAPER_WIDTH = 380;

    private final ImageView page = new ImageView();
    private final Label message = new Label();
    private final PauseTransition delay = new PauseTransition(DELAY);
    private final ExecutorService renderer = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "invoice-preview");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicLong latestRequest = new AtomicLong();

    PreviewPane() {
        Label caption = new Label(Messages.get("preview.title"));
        caption.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.TEXT_MUTED);

        page.setPreserveRatio(true);
        page.setFitWidth(PAPER_WIDTH);
        page.setSmooth(true);

        message.setWrapText(true);
        message.setMaxWidth(PAPER_WIDTH - 48);
        message.getStyleClass().add(Styles.TEXT_MUTED);

        StackPane paper = new StackPane(page, message);
        paper.setAlignment(Pos.CENTER);
        paper.setMinSize(PAPER_WIDTH, PAPER_WIDTH * 297 / 210);
        paper.setMaxWidth(PAPER_WIDTH);
        paper.getStyleClass().add("paper");

        setSpacing(8);
        getChildren().addAll(caption, paper);
    }

    /**
     * Renders the PDF supplied by the given source after a short pause, so typing does not trigger
     * a rendering for every key stroke. Only the result of the latest request is shown.
     */
    void render(Callable<byte[]> pdfSource) {
        long request = latestRequest.incrementAndGet();
        delay.setOnFinished(event -> renderer.submit(() -> renderInBackground(request, pdfSource)));
        delay.playFromStart();
    }

    /** Replaces the preview by a text, used while mandatory details are missing. */
    void showMessage(String text) {
        latestRequest.incrementAndGet();
        delay.stop();
        page.setImage(null);
        message.setText(text);
    }

    /** Shuts the background thread down; the pane must not be used afterwards. */
    void dispose() {
        delay.stop();
        renderer.shutdownNow();
    }

    private void renderInBackground(long request, Callable<byte[]> pdfSource) {
        if (request != latestRequest.get()) {
            return;
        }
        try {
            Image image = SwingFXUtils.toFXImage(PdfBuilder.renderFirstPage(pdfSource.call(), RENDER_DPI), null);
            Platform.runLater(() -> {
                if (request == latestRequest.get()) {
                    message.setText("");
                    page.setImage(image);
                }
            });
        } catch (Exception e) {
            Platform.runLater(() -> {
                if (request == latestRequest.get()) {
                    showMessage(Messages.get("preview.failed") + "\n" + e.getMessage());
                }
            });
        }
    }
}
