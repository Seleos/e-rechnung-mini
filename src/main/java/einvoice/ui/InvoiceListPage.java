package einvoice.ui;

import atlantafx.base.theme.Styles;
import einvoice.core.Numbers;
import einvoice.i18n.Messages;
import einvoice.model.Customer;
import einvoice.model.InvoiceData;

import java.math.BigDecimal;
import java.text.Collator;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * Page listing the invoices created with this app for the selected company. A click on a column heading sorts by
 * that column; the newest number comes first by default. A double click on a row opens the invoice PDF.
 */
final class InvoiceListPage extends Page {
    private final ObservableList<InvoiceData> records = FXCollections.observableArrayList();
    private final FilteredList<InvoiceData> visibleRecords = new FilteredList<>(records);
    // A filtered list cannot be sorted itself; the table sorts this view of it instead.
    private final SortedList<InvoiceData> sortedRecords = new SortedList<>(visibleRecords);
    private final TableView<InvoiceData> table = new TableView<>(sortedRecords);
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

        Comparator<String> byName = Collator.getInstance(Messages.language().locale())::compare;
        TableColumn<InvoiceData, InvoiceData> number = column(Messages.get("invoices.column.number"), 150,
                InvoiceData::getNumber, InvoiceData::getNumber, Comparator.naturalOrder());
        table.getColumns().add(number);
        table.getColumns().add(column(Messages.get("invoices.column.date"), 110,
                InvoiceListPage::invoiceDate, InvoiceData::getDate, Comparator.naturalOrder()));
        table.getColumns().add(column(Messages.get("editor.customer"), 280,
                InvoiceListPage::customerName, InvoiceListPage::customerName, byName));
        table.getColumns().add(amountColumn(Messages.get("invoices.column.net"), InvoiceData::getNet));
        table.getColumns().add(amountColumn(Messages.get("invoices.column.gross"), InvoiceData::getGross));
        sortedRecords.comparatorProperty().bind(table.comparatorProperty());
        number.setSortType(TableColumn.SortType.DESCENDING);
        table.getSortOrder().add(number);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(hint(Messages.get("invoices.empty")));
        table.setPrefHeight(520);
        table.getStyleClass().add(Styles.STRIPED);

        Button template = new Button(Messages.get("invoices.useAsTemplate"));
        template.setOnAction(event -> useAsTemplate.accept(table.getSelectionModel().getSelectedItem()));
        InvoiceActionBar actionBar = new InvoiceActionBar(context, () -> table.getSelectionModel().getSelectedItem());
        actionBar.add(template);
        actionBar.disableWhile(table.getSelectionModel().selectedItemProperty().isNull());
        table.setRowFactory(view -> {
            TableRow<InvoiceData> row = new TableRow<>();
            // The first click of the double click has already selected the row, so the bar opens this invoice.
            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 && !row.isEmpty()) {
                    actionBar.openPdf();
                }
            });
            return row;
        });

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

    /**
     * Creates a column that shows formatted text but sorts by the value behind it, so dates and amounts sort
     * by date and size instead of alphabetically.
     */
    private static <T> TableColumn<InvoiceData, InvoiceData> column(String title, double width,
                                                                   Function<InvoiceData, String> text,
                                                                   Function<InvoiceData, T> sortValue,
                                                                   Comparator<T> order) {
        TableColumn<InvoiceData, InvoiceData> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        column.setCellFactory(ignored -> new TableCell<>() {
            @Override
            protected void updateItem(InvoiceData record, boolean empty) {
                super.updateItem(record, empty);
                setText(empty || record == null ? null : text.apply(record));
            }
        });
        column.setComparator(Comparator.comparing(sortValue, Comparator.nullsFirst(order)));
        column.setPrefWidth(width);
        return column;
    }

    private static TableColumn<InvoiceData, InvoiceData> amountColumn(String title,
                                                                      Function<InvoiceData, BigDecimal> amount) {
        TableColumn<InvoiceData, InvoiceData> column = column(title, 130,
                record -> money(record, amount.apply(record)), amount, Comparator.naturalOrder());
        column.getStyleClass().add("amount-column");
        return column;
    }
}
