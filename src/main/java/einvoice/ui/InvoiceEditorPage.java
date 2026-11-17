package einvoice.ui;

import atlantafx.base.theme.Styles;
import einvoice.core.InputCheck;
import einvoice.core.Numbers;
import einvoice.core.Tax;
import einvoice.core.XmlBuilder;
import einvoice.i18n.Messages;
import einvoice.model.Company;
import einvoice.model.Customer;
import einvoice.model.InvoiceData;
import einvoice.model.LineItem;
import einvoice.model.Service;
import einvoice.service.InvoiceService.InvalidInvoiceException;

import java.math.BigDecimal;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Page for writing an invoice: customer, dates, line items, totals and a live preview of the PDF. */
final class InvoiceEditorPage extends Page {
    static final String CREATE_BUTTON_ID = "create-invoice";
    static final String RESULT_BOX_ID = "invoice-result";
    static final String CUSTOMER_BOX_ID = "invoice-customer";

    private final ComboBox<Customer> customer = new ComboBox<>();
    private final Label taxCase = new Label();
    private final TextField number = new TextField();
    private final DatePicker date = new DatePicker();
    private final DatePicker serviceFrom = new DatePicker();
    private final DatePicker serviceTo = new DatePicker();
    private final VBox rows = new VBox(8);
    private final MenuButton serviceMenu = new MenuButton(Messages.get("editor.insertService"));
    private final Label net = new Label();
    private final Label taxLabel = new Label();
    private final Label tax = new Label();
    private final Label gross = new Label();
    private final VBox problems = new VBox(4);
    private final Button create = new Button(Messages.get("editor.create"));
    private final VBox result = new VBox(12);
    private final PreviewPane preview = new PreviewPane();
    /** Suppresses change handling while fields are filled programmatically. */
    private boolean loading;
    /** Company the page currently shows; a change resets the page. */
    private String shownCompanyId;
    /** Invoice created last, kept for the e-mail buttons of the confirmation. */
    private InvoiceData createdInvoice;

    InvoiceEditorPage(AppContext context) {
        super(context);
        VBox form = new VBox(16, header(), card(headerFields()), card(itemSection()), problems, actions(), result);
        HBox.setHgrow(form, Priority.ALWAYS);
        form.setMinWidth(560);
        HBox columns = new HBox(24, form, preview);
        getChildren().add(columns);

        customer.setId(CUSTOMER_BOX_ID);
        customer.valueProperty().addListener((property, old, value) -> customerChanged(old));
        number.textProperty().addListener((property, old, value) -> changed());
        date.valueProperty().addListener((property, old, value) -> changed());
        serviceFrom.valueProperty().addListener((property, old, value) -> changed());
        serviceTo.valueProperty().addListener((property, old, value) -> changed());
        create.setId(CREATE_BUTTON_ID);
        create.getStyleClass().add(Styles.ACCENT);
        create.setOnAction(event -> createInvoice());
        problems.getStyleClass().add("problem-box");
        result.setId(RESULT_BOX_ID);
        result.getStyleClass().add("result-box");
        shownCompanyId = context.company().getId();
        startNewInvoice();
    }

    @Override
    void refresh() {
        if (companyChanged()) {
            // Lines, number and confirmation belong to the previous company, so the page starts over.
            shownCompanyId = context().company().getId();
            startNewInvoice();
            return;
        }
        loading = true;
        Customer selected = customer.getValue();
        List<Customer> customers = context().store().customers(context().company());
        customer.getItems().setAll(customers);
        customer.setValue(customers.stream()
                .filter(candidate -> selected != null && candidate.getId().equals(selected.getId()))
                .findFirst()
                .orElse(customers.isEmpty() ? null : customers.get(0)));
        fillServiceMenu();
        boolean invoiceJustCreated = result.isVisible();
        if (!invoiceJustCreated) {
            number.setText(proposedNumber());
        }
        loading = false;
        if (!invoiceJustCreated) {
            changed();
        }
    }

    private boolean companyChanged() {
        return shownCompanyId != null && !shownCompanyId.equals(context().company().getId());
    }

    /** The next free number for the year of the invoice date, or of today while no date is entered. */
    private String proposedNumber() {
        LocalDate invoiceDate = date.getValue() == null ? LocalDate.now() : date.getValue();
        return context().store().nextNumber(context().company(), invoiceDate);
    }

    /** Stops background work of this page; called before the window is replaced. */
    void dispose() {
        preview.dispose();
    }

    /** Clears the page for a new invoice: today's date, last month as service period, one empty line. */
    void startNewInvoice() {
        loading = true;
        LocalDate today = LocalDate.now();
        LocalDate lastMonth = today.minusMonths(1);
        date.setValue(today);
        serviceFrom.setValue(lastMonth.withDayOfMonth(1));
        serviceTo.setValue(lastMonth.withDayOfMonth(lastMonth.lengthOfMonth()));
        rows.getChildren().clear();
        showResult(null);
        problems.getChildren().clear();
        loading = false;
        refresh();
        showDefaultLines();
    }

    /** Starts a new invoice with customer and line items copied from an earlier one. */
    void loadTemplate(InvoiceData template) {
        startNewInvoice();
        loading = true;
        if (template.getCustomer() != null) {
            // A customer deleted since then is offered again from the copy stored with the invoice.
            Customer recipient = customer.getItems().stream()
                    .filter(candidate -> candidate.getId().equals(template.getCustomer().getId()))
                    .findFirst()
                    .orElseGet(() -> {
                        customer.getItems().add(template.getCustomer());
                        return template.getCustomer();
                    });
            customer.setValue(recipient);
        }
        rows.getChildren().clear();
        loading = false;
        template.getItems().forEach(this::addRow);
    }

    private Node header() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        number.setPrefWidth(170);
        number.setAccessibleText(Messages.get("editor.number"));
        Label numberCaption = new Label(Messages.get("editor.numberShort"));
        HBox header = new HBox(12, title(Messages.get("nav.newInvoice")), spacer, numberCaption, number);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private Node headerFields() {
        customer.setMaxWidth(Double.MAX_VALUE);
        // As high as the box next to it, so the text sits on the same line as the customer name.
        taxCase.minHeightProperty().bind(customer.heightProperty());
        date.setMaxWidth(Double.MAX_VALUE);
        serviceFrom.setMaxWidth(Double.MAX_VALUE);
        serviceTo.setMaxWidth(Double.MAX_VALUE);
        Form<Void> form = new Form<>(3);
        form.custom(Messages.get("editor.customer"), customer, 2, ignored -> { }, ignored -> { });
        form.custom(Messages.get("field.taxCase"), taxCase, 1, ignored -> { }, ignored -> { });
        form.custom(Messages.get("editor.date"), date, 1, ignored -> { }, ignored -> { });
        form.custom(Messages.get("editor.serviceFrom"), serviceFrom, 1, ignored -> { }, ignored -> { });
        form.custom(Messages.get("editor.serviceTo"), serviceTo, 1, ignored -> { }, ignored -> { });
        return form.node();
    }

    private Node itemSection() {
        Label heading = new Label(Messages.get("editor.items"));
        heading.getStyleClass().add(Styles.TITLE_4);

        Button addItem = new Button(Messages.get("editor.addItem"));
        addItem.setOnAction(event -> addRow(new LineItem(null, "h", "", BigDecimal.ZERO)));
        HBox buttons = new HBox(8, addItem, serviceMenu);

        GridPane totals = new GridPane();
        totals.setHgap(32);
        totals.setVgap(4);
        Label grossCaption = new Label(Messages.get("editor.gross"));
        grossCaption.getStyleClass().add(Styles.TEXT_BOLD);
        gross.getStyleClass().add(Styles.TEXT_BOLD);
        totals.addRow(0, new Label(Messages.get("editor.net")), net);
        totals.addRow(1, taxLabel, tax);
        totals.addRow(2, grossCaption, gross);
        for (Label amount : List.of(net, tax, gross)) {
            amount.setMinWidth(110);
            amount.setAlignment(Pos.CENTER_RIGHT);
        }
        HBox totalsRow = new HBox(totals);
        totalsRow.setAlignment(Pos.CENTER_RIGHT);

        return new VBox(12, heading, columnHeadings(), rows, buttons, totalsRow);
    }

    private static Node columnHeadings() {
        Label text = new Label(Messages.get("editor.column.text"));
        HBox.setHgrow(text, Priority.ALWAYS);
        text.setMaxWidth(Double.MAX_VALUE);
        HBox headings = new HBox(8,
                heading(Messages.get("editor.column.position"), LineItemRow.POSITION_WIDTH, Pos.CENTER_LEFT),
                heading(Messages.get("editor.column.quantity"), LineItemRow.QUANTITY_WIDTH, Pos.CENTER_LEFT),
                heading(Messages.get("field.unit"), LineItemRow.UNIT_WIDTH, Pos.CENTER_LEFT),
                text,
                heading(Messages.get("editor.column.unitPrice"), LineItemRow.PRICE_WIDTH, Pos.CENTER_RIGHT),
                heading(Messages.get("editor.column.total"), LineItemRow.TOTAL_WIDTH, Pos.CENTER_RIGHT),
                heading("", LineItemRow.REMOVE_WIDTH, Pos.CENTER_LEFT));
        headings.getChildren().forEach(node -> node.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.TEXT_MUTED));
        return headings;
    }

    private static Label heading(String text, double width, Pos alignment) {
        Label label = new Label(text);
        label.setMinWidth(width);
        label.setPrefWidth(width);
        label.setAlignment(alignment);
        return label;
    }

    private Node actions() {
        HBox actions = new HBox(create);
        actions.setAlignment(Pos.CENTER_RIGHT);
        return actions;
    }

    private static VBox card(Node content) {
        VBox card = new VBox(content);
        card.getStyleClass().add("card");
        return card;
    }

    private void fillServiceMenu() {
        serviceMenu.getItems().clear();
        List<Service> services = customer.getValue() == null ? List.of() : customer.getValue().getServices();
        for (Service service : services) {
            MenuItem entry = new MenuItem(service.getText() + " ("
                    + Numbers.money(service.getUnitPrice(), customer.getValue().getCurrency()) + ")");
            entry.setOnAction(event -> addRow(
                    new LineItem(null, service.getUnit(), service.getText(), service.getUnitPrice())));
            serviceMenu.getItems().add(entry);
        }
        serviceMenu.setDisable(serviceMenu.getItems().isEmpty());
    }

    /** Offers the services of the new customer and replaces a first line the user has not touched yet. */
    private void customerChanged(Customer previous) {
        fillServiceMenu();
        if (loading) {
            return;
        }
        if (showsOnlyDefaultLines(previous)) {
            showDefaultLines();
        } else {
            changed();
        }
    }

    /** Replaces all lines by the pre-filled lines of the selected customer and waits for the first quantity. */
    private void showDefaultLines() {
        rows.getChildren().clear();
        defaultLines(customer.getValue()).forEach(this::addRow);
        Platform.runLater(() -> {
            if (!rows.getChildren().isEmpty()) {
                ((LineItemRow) rows.getChildren().get(0)).focusQuantity();
            }
        });
    }

    /**
     * Lines a new invoice starts with: the services the customer has marked as pre-filled, without a
     * quantity, or one empty line if there are none.
     */
    private static List<LineItem> defaultLines(Customer selected) {
        List<LineItem> lines = new ArrayList<>();
        if (selected != null) {
            for (Service service : selected.getServices()) {
                if (service.isIncludedByDefault()) {
                    lines.add(new LineItem(null, service.getUnit(), service.getText(), service.getUnitPrice()));
                }
            }
        }
        if (lines.isEmpty()) {
            lines.add(new LineItem(null, "h", "", BigDecimal.ZERO));
        }
        return lines;
    }

    /** True while the user has not changed the pre-filled lines, so they may be replaced safely. */
    private boolean showsOnlyDefaultLines(Customer previous) {
        List<LineItem> expected = defaultLines(previous);
        if (rows.getChildren().size() != expected.size()) {
            return false;
        }
        for (int index = 0; index < expected.size(); index++) {
            LineItem line = ((LineItemRow) rows.getChildren().get(index)).item();
            LineItem original = expected.get(index);
            boolean samePrice = line.getUnitPrice() != null
                    && line.getUnitPrice().compareTo(original.getUnitPrice()) == 0;
            if (line.getQuantity() != null || !line.getText().equals(original.getText()) || !samePrice) {
                return false;
            }
        }
        return true;
    }

    private void addRow(LineItem item) {
        LineItemRow[] holder = new LineItemRow[1];
        holder[0] = new LineItemRow(item, this::changed, () -> {
            rows.getChildren().remove(holder[0]);
            changed();
        });
        rows.getChildren().add(holder[0]);
        changed();
    }

    /** Recalculates totals and refreshes the preview after any input changed. */
    private void changed() {
        if (loading || context().company() == null) {
            return;
        }
        if (result.isVisible()) {
            continueAfterCreatedInvoice();
        }
        numberRows();
        Company company = context().company();
        InvoiceData invoice = currentInvoice();
        Tax taxRule = Tax.of(company, invoice.getCustomer());
        taxCase.setText(taxRule.label());
        taxLabel.setText(taxRule.rowLabel());

        List<String> errors = InputCheck.errors(company, invoice);
        if (!errors.isEmpty()) {
            net.setText("");
            tax.setText("");
            gross.setText("");
            preview.showMessage(Messages.get("editor.previewMissing") + "\n\n" + String.join("\n", errors));
            return;
        }
        problems.getChildren().clear();
        XmlBuilder.Totals totals = XmlBuilder.totals(company, invoice);
        String currency = invoice.getCustomer().getCurrency();
        net.setText(Numbers.money(totals.net(), currency));
        tax.setText(Numbers.money(totals.tax(), currency));
        gross.setText(Numbers.money(totals.gross(), currency));
        preview.render(() -> context().invoiceService().preview(company, invoice));
    }

    /** Editing after an invoice was created starts the next one, so it needs the next free number. */
    private void continueAfterCreatedInvoice() {
        showResult(null);
        loading = true;
        number.setText(proposedNumber());
        loading = false;
    }

    private void numberRows() {
        int position = 1;
        for (Node row : rows.getChildren()) {
            ((LineItemRow) row).setPosition(position++);
        }
    }

    private InvoiceData currentInvoice() {
        InvoiceData invoice = new InvoiceData();
        invoice.setNumber(number.getText() == null ? "" : number.getText().trim());
        invoice.setDate(date.getValue());
        invoice.setServiceFrom(serviceFrom.getValue());
        invoice.setServiceTo(serviceTo.getValue());
        invoice.setCustomer(customer.getValue() == null ? null : customer.getValue().copy());
        List<LineItem> items = new ArrayList<>();
        for (Node row : rows.getChildren()) {
            items.add(((LineItemRow) row).item());
        }
        invoice.setItems(items);
        return invoice;
    }

    private void createInvoice() {
        Company company = context().company();
        InvoiceData invoice = currentInvoice();
        List<String> errors = InputCheck.errors(company, invoice);
        if (!errors.isEmpty()) {
            showProblems(Messages.get("editor.pleaseComplete"), errors);
            return;
        }
        Task<Path> task = new Task<>() {
            @Override
            protected Path call() throws Exception {
                return context().invoiceService().create(company, invoice);
            }
        };
        task.setOnSucceeded(event -> {
            create.setDisable(false);
            create.setText(Messages.get("editor.create"));
            createdInvoice = invoice;
            showResult(task.getValue());
        });
        task.setOnFailed(event -> {
            create.setDisable(false);
            create.setText(Messages.get("editor.create"));
            showFailure(task.getException());
        });
        create.setDisable(true);
        create.setText(Messages.get("editor.creating"));
        Thread thread = new Thread(task, "invoice-create");
        thread.setDaemon(true);
        thread.start();
    }

    private void showFailure(Throwable failure) {
        if (failure instanceof InvalidInvoiceException invalid) {
            showProblems(Messages.get("editor.invalid"),
                    invalid.result().problems());
        } else if (failure instanceof FileAlreadyExistsException) {
            showProblems(Messages.get("editor.notSaved"),
                    List.of(Messages.get("editor.numberExists", failure.getMessage())));
        } else {
            Dialogs.error(Messages.get("editor.createFailed"), failure);
        }
    }

    private void showProblems(String headline, List<String> messages) {
        Label heading = new Label(headline);
        heading.getStyleClass().add(Styles.TEXT_BOLD);
        problems.getChildren().setAll(heading);
        for (String message : messages) {
            Label line = new Label("• " + message);
            line.setWrapText(true);
            problems.getChildren().add(line);
        }
    }

    /** Shows the confirmation with follow-up actions, or hides it when the file is null. */
    private void showResult(Path file) {
        result.getChildren().clear();
        result.setVisible(file != null);
        result.setManaged(file != null);
        create.setVisible(file == null);
        create.setManaged(file == null);
        if (file == null) {
            return;
        }
        Label message = new Label(Messages.get("editor.created", file.getFileName()));
        message.getStyleClass().add(Styles.TEXT_BOLD);
        message.setWrapText(true);
        Button next = new Button(Messages.get("editor.next"));
        next.getStyleClass().add(Styles.ACCENT);
        next.setOnAction(event -> startNewInvoice());
        InvoiceActionBar actions = new InvoiceActionBar(context(), () -> createdInvoice);
        actions.add(next);
        result.getChildren().addAll(message, actions);
    }
}
