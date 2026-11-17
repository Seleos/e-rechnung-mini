package einvoice.ui;

import atlantafx.base.theme.Styles;
import einvoice.i18n.Messages;
import einvoice.model.Company;
import einvoice.model.InvoiceData;

import java.util.EnumMap;
import java.util.Map;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Main window: sidebar with company switcher and navigation on the left, the selected page on the right. */
final class MainWindow extends BorderPane {

    /** Pages reachable from the sidebar. */
    enum PageId {
        EDITOR("nav.newInvoice"),
        INVOICES("nav.invoices"),
        CUSTOMERS("nav.customers"),
        SETTINGS("nav.settings");

        private final String messageKey;

        PageId(String messageKey) {
            this.messageKey = messageKey;
        }
    }

    private static final double SIDEBAR_WIDTH = 236;

    private final AppContext context;
    private final Map<PageId, Page> pages = new EnumMap<>(PageId.class);
    private final Map<PageId, ToggleButton> navigationButtons = new EnumMap<>(PageId.class);
    private final ToggleGroup navigation = new ToggleGroup();
    private final ScrollPane content = new ScrollPane();
    private final InvoiceEditorPage editor;
    private final ComboBox<Company> companySwitcher = new ComboBox<>();
    private final ChangeListener<Company> companyListener = (property, old, company) -> refreshCurrentPage();

    MainWindow(AppContext context) {
        this.context = context;
        editor = new InvoiceEditorPage(context);
        pages.put(PageId.EDITOR, editor);
        pages.put(PageId.INVOICES, new InvoiceListPage(context, this::useAsTemplate));
        pages.put(PageId.CUSTOMERS, new CustomerPage(context));
        pages.put(PageId.SETTINGS, new SettingsPage(context));

        content.setFitToWidth(true);
        content.getStyleClass().add("page-scroll");
        setLeft(sidebar());
        setCenter(content);

        context.companyProperty().addListener(companyListener);
        show(PageId.EDITOR);
    }

    /** Detaches this window from the shared state; called before it is replaced after a language change. */
    void dispose() {
        editor.dispose();
        context.companyProperty().removeListener(companyListener);
        companySwitcher.valueProperty().unbindBidirectional(context.companyProperty());
    }

    /** Shows the given page and reloads its data. */
    void show(PageId pageId) {
        Page page = pages.get(pageId);
        page.refresh();
        content.setContent(page);
        reportDamagedFiles();
        content.setVvalue(0);
        // The editor is reached through the "Neue Rechnung" button and has no navigation entry of its own.
        navigation.selectToggle(navigationButtons.get(pageId));
    }

    /** Tells the user once about data files that had to be skipped because they are damaged. */
    private void reportDamagedFiles() {
        String files = context.takeStorageWarning();
        if (!files.isEmpty()) {
            Platform.runLater(() -> Dialogs.error(Messages.get("storage.unreadable"), files));
        }
    }

    private void refreshCurrentPage() {
        if (context.company() != null && content.getContent() instanceof Page page) {
            page.refresh();
        }
    }

    private void useAsTemplate(InvoiceData record) {
        show(PageId.EDITOR);
        editor.loadTemplate(record);
    }

    private VBox sidebar() {
        Label caption = new Label(Messages.get("nav.invoiceFrom"));
        caption.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.TEXT_MUTED);

        companySwitcher.setItems(context.companies());
        companySwitcher.setMaxWidth(Double.MAX_VALUE);
        companySwitcher.valueProperty().bindBidirectional(context.companyProperty());

        Button newInvoice = new Button(Messages.get("nav.newInvoice"));
        newInvoice.getStyleClass().add(Styles.ACCENT);
        newInvoice.setMaxWidth(Double.MAX_VALUE);
        newInvoice.setOnAction(event -> {
            show(PageId.EDITOR);
            editor.startNewInvoice();
        });

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox sidebar = new VBox(8,
                caption, companySwitcher, new Region(), newInvoice, new Region(),
                navigationButton(PageId.INVOICES),
                navigationButton(PageId.CUSTOMERS),
                spacer, new Separator(),
                navigationButton(PageId.SETTINGS));
        sidebar.setPadding(new Insets(16));
        sidebar.setPrefWidth(SIDEBAR_WIDTH);
        sidebar.setMinWidth(SIDEBAR_WIDTH);
        sidebar.getStyleClass().add("sidebar");
        return sidebar;
    }

    private ToggleButton navigationButton(PageId pageId) {
        ToggleButton button = new ToggleButton(Messages.get(pageId.messageKey));
        button.setToggleGroup(navigation);
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().addAll(Styles.FLAT, "navigation-button");
        button.setOnAction(event -> show(pageId));
        navigationButtons.put(pageId, button);
        return button;
    }
}
