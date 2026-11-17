package einvoice.ui;

import atlantafx.base.theme.Styles;
import einvoice.core.Numbers;
import einvoice.i18n.Messages;
import einvoice.model.Customer;
import einvoice.model.InvoiceData;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/** Page listing the invoices created with this app for the selected company. */
final class InvoiceListPage extends Page {
    private final ObservableList<InvoiceData> records = FXCollections.observableArrayList();
    private final FilteredList<InvoiceData> visibleRecords = new FilteredList<>(records);
    private final TableView<InvoiceData> table = new TableView<>(visibleRecords);
    private final TextField search = new TextField();

    InvoiceListPage(AppContext context, Consumer<InvoiceData> useAsTemplate) {
        super(context);
        search.setPromptText(Messages.get("invoices.search"));
        search.setPrefWidth(280);
        search.textProperty().addListener((property, old, value) -> applyFilter());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(12, title(Messages.get("nav.invoices")), spacer, search);
        header.setAlignment(Pos.CENTER_LEFT);

        table.getColumns().add(column(Messages.get("invoices.column.number"), 150, InvoiceData::getNumber));
        table.getColumns().add(column(Messages.get("invoices.column.date"), 110, InvoiceListPage::invoiceDate));
        table.getColumns().add(column(Messages.get("editor.customer"), 280, InvoiceListPage::customerName));
        table.getColumns().add(amountColumn(Messages.get("invoices.column.net"),
                record -> money(record, record.getNet())));
        table.getColumns().add(amountColumn(Messages.get("invoices.column.gross"),
                record -> money(record, record.getGross())));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(hint(Messages.get("invoices.empty")));
        table.setPrefHeight(520);
        table.getStyleClass().add(Styles.STRIPED);

        Button template = new Button(Messages.get("invoices.useAsTemplate"));
        template.setOnAction(event -> useAsTemplate.accept(table.getSelectionModel().getSelectedItem()));
        InvoiceActionBar actionBar = new InvoiceActionBar(context, () -> table.getSelectionModel().getSelectedItem());
        actionBar.add(template);
        actionBar.disableWhile(table.getSelectionModel().selectedItemProperty().isNull());

        getChildren().addAll(header, table, actionBar,
                hint(Messages.get("invoices.hint")));
    }

    @Override
    void refresh() {
        records.setAll(context().store().records(context().company()));
        // The newest invoice is selected right away, so the buttons below are ready to use.
        table.getSelectionModel().selectFirst();
    }

    private void applyFilter() {
        String query = search.getText() == null ? "" : search.getText().trim().toLowerCase();
        visibleRecords.setPredicate(record -> query.isEmpty()
                || record.getNumber().toLowerCase().contains(query)
                || customerName(record).toLowerCase().contains(query));
    }

    private static String invoiceDate(InvoiceData record) {
        if (record.getDate() == null) {
            return "";
        }
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(Messages.language().locale())
                .format(record.getDate());
    }

    private static String money(InvoiceData record, BigDecimal amount) {
        String currency = record.getCustomer() == null ? Customer.DEFAULT_CURRENCY : record.getCustomer().getCurrency();
        return Numbers.money(amount, currency);
    }

    private static String customerName(InvoiceData record) {
        return record.getCustomer() == null ? "" : record.getCustomer().getName();
    }

    private static TableColumn<InvoiceData, String> column(String title, double width,
                                                           Function<InvoiceData, String> value) {
        TableColumn<InvoiceData, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    private static TableColumn<InvoiceData, String> amountColumn(String title, Function<InvoiceData, String> value) {
        TableColumn<InvoiceData, String> column = column(title, 130, value);
        column.getStyleClass().add("amount-column");
        column.setSortable(false);
        return column;
    }
}
