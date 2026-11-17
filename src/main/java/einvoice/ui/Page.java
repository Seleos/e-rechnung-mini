package einvoice.ui;

import atlantafx.base.theme.Styles;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Base class of the content pages shown next to the sidebar. */
abstract class Page extends VBox {
    private static final double SPACING = 20;
    private static final Insets PADDING = new Insets(28, 32, 32, 32);

    private final AppContext context;

    Page(AppContext context) {
        this.context = context;
        setSpacing(SPACING);
        setPadding(PADDING);
    }

    /** Reloads the page from the data folder; called when the page is shown or the company changes. */
    abstract void refresh();

    AppContext context() {
        return context;
    }

    static Label title(String text) {
        Label title = new Label(text);
        title.getStyleClass().add(Styles.TITLE_2);
        return title;
    }

    static Label hint(String text) {
        Label hint = new Label(text);
        hint.getStyleClass().add(Styles.TEXT_MUTED);
        hint.setWrapText(true);
        return hint;
    }
}
