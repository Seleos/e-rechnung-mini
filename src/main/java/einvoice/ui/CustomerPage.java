package einvoice.ui;

import atlantafx.base.theme.Styles;
import einvoice.core.EmailText;
import einvoice.core.Placeholders;
import einvoice.i18n.Language;
import einvoice.i18n.Messages;
import einvoice.model.Customer;
import einvoice.model.Service;
import einvoice.model.TaxCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Page for maintaining the customers of the selected company together with their services and rates. */
final class CustomerPage extends Page {
    private static final List<String> COMMON_CURRENCIES = List.of("EUR", "USD", "CHF", "GBP");

    private final ListView<Customer> list = new ListView<>();
    private final Form<Customer> form = new Form<>(2);
    private final ComboBox<TaxCase> taxCase = new ComboBox<>();
    private final ComboBox<Language> invoiceLanguage = new ComboBox<>();
    private final ComboBox<String> currency = new ComboBox<>();
    private final Form<Customer> emailForm = new Form<>(1);
    private final VBox serviceRows = new VBox(8);
    /** Customer currently shown in the form; a new one has no id yet. */
    private Customer current;

    CustomerPage(AppContext context) {
        super(context);
        buildForm();

        Button add = new Button(Messages.get("customers.add"));
        add.setOnAction(event -> edit(new Customer()));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(12, title(Messages.get("nav.customers")), spacer, add);
        header.setAlignment(Pos.CENTER_LEFT);

        list.setPrefWidth(260);
        list.setMinWidth(220);
        list.setPrefHeight(520);
        list.getSelectionModel().selectedItemProperty().addListener((property, old, selected) -> {
            if (selected != null) {
                edit(selected);
            }
        });

        Button save = new Button(Messages.get("action.save"));
        save.getStyleClass().add(Styles.ACCENT);
        save.setOnAction(event -> save());
        Button delete = new Button(Messages.get("action.delete"));
        delete.getStyleClass().addAll(Styles.FLAT, Styles.DANGER);
        delete.setOnAction(event -> delete());
        HBox buttons = new HBox(8, save, delete);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox masterData = new VBox(form.node());
        masterData.getStyleClass().add("card");
        VBox editor = new VBox(16, masterData, serviceSection(), emailSection(), buttons);
        HBox.setHgrow(editor, Priority.ALWAYS);
        getChildren().addAll(header, new HBox(20, list, editor));
    }

    @Override
    void refresh() {
        List<Customer> customers = context().store().customers(context().company());
        String selectedId = current == null ? "" : current.getId();
        list.getItems().setAll(customers);
        Customer selection = customers.stream()
                .filter(candidate -> candidate.getId().equals(selectedId))
                .findFirst()
                .orElse(customers.isEmpty() ? null : customers.get(0));
        if (selection == null) {
            edit(new Customer());
        } else {
            list.getSelectionModel().select(selection);
        }
    }

    private void buildForm() {
        taxCase.getItems().setAll(TaxCase.values());
        invoiceLanguage.getItems().setAll(Language.values());
        invoiceLanguage.setMaxWidth(Double.MAX_VALUE);
        currency.getItems().setAll(COMMON_CURRENCIES);
        currency.setEditable(true);
        currency.setMaxWidth(Double.MAX_VALUE);
        taxCase.setMaxWidth(Double.MAX_VALUE);
        form.wideText(Messages.get("customers.name"), Customer::getName, Customer::setName);
        form.wideText(Messages.get("customers.addressExtra"), Customer::getAddressExtra, Customer::setAddressExtra);
        form.wideText(Messages.get("field.street"), Customer::getStreet, Customer::setStreet);
        form.text(Messages.get("field.zip"), Customer::getZip, Customer::setZip);
        form.text(Messages.get("field.city"), Customer::getCity, Customer::setCity);
        form.text(Messages.get("customers.country"), Customer::getCountry,
                (customer, value) -> customer.setCountry(value.toUpperCase()));
        form.text(Messages.get("field.vatId"), Customer::getVatId, Customer::setVatId);
        form.text(Messages.get("customers.email"), Customer::getEmail, Customer::setEmail);
        form.custom(Messages.get("field.taxCase"), taxCase, 1,
                customer -> taxCase.setValue(customer.getTaxCase()),
                customer -> customer.setTaxCase(taxCase.getValue()));
        form.custom(Messages.get("customers.invoiceLanguage"), invoiceLanguage, 1,
                customer -> invoiceLanguage.setValue(customer.getInvoiceLanguage()),
                customer -> customer.setInvoiceLanguage(invoiceLanguage.getValue()));
        form.custom(Messages.get("customers.currency"), currency, 1,
                customer -> currency.setValue(customer.getCurrency()),
                customer -> customer.setCurrency(currency.getEditor().getText().trim().toUpperCase()));
        form.text(Messages.get("customers.buyerReference"), Customer::getBuyerReference, Customer::setBuyerReference);
    }

    private VBox emailSection() {
        Label heading = new Label(Messages.get("customers.email.title"));
        heading.getStyleClass().add(Styles.TITLE_4);
        TextField subject = emailForm.wideText(Messages.get("customers.email.subject"),
                Customer::getEmailSubject, Customer::setEmailSubject);
        subject.setPromptText(Messages.get("customers.email.prompt"));
        TextArea body = emailForm.textArea(Messages.get("customers.email.body"),
                Customer::getEmailBody, Customer::setEmailBody);
        body.setPromptText(Messages.get("customers.email.prompt"));
        body.setPrefRowCount(8);

        VBox section = new VBox(12, heading, emailForm.node(),
                hint(Messages.get("customers.email.hint", String.join(", ", EmailText.PLACEHOLDERS))));
        section.getStyleClass().add("card");
        return section;
    }

    private VBox serviceSection() {
        Label heading = new Label(Messages.get("customers.services"));
        heading.getStyleClass().add(Styles.TITLE_4);

        Label text = new Label(Messages.get("customers.serviceName"));
        text.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(text, Priority.ALWAYS);
        Label unit = new Label(Messages.get("field.unit"));
        unit.setMinWidth(ServiceRow.UNIT_WIDTH);
        Label price = new Label(Messages.get("customers.rate"));
        price.setMinWidth(ServiceRow.PRICE_WIDTH);
        price.setAlignment(Pos.CENTER_RIGHT);
        Label defaultColumn = new Label(Messages.get("customers.serviceDefault"));
        defaultColumn.setMinWidth(ServiceRow.DEFAULT_WIDTH);
        defaultColumn.setAlignment(Pos.CENTER);
        Region removeColumn = new Region();
        removeColumn.setMinWidth(ServiceRow.REMOVE_WIDTH);
        HBox headings = new HBox(8, text, unit, price, defaultColumn, removeColumn);
        headings.getChildren().forEach(node -> node.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.TEXT_MUTED));

        Button add = new Button(Messages.get("customers.addService"));
        add.setOnAction(event -> addServiceRow(new Service()));

        VBox section = new VBox(12, heading,
                hint(Messages.get("customers.servicesHint")),
                hint(Messages.get("placeholders.hint", String.join(", ", Placeholders.NAMES))),
                headings, serviceRows, add);
        section.getStyleClass().add("card");
        return section;
    }

    private void addServiceRow(Service service) {
        ServiceRow[] holder = new ServiceRow[1];
        holder[0] = new ServiceRow(service, () -> serviceRows.getChildren().remove(holder[0]));
        serviceRows.getChildren().add(holder[0]);
    }

    /** Services as entered; rows without a name are dropped. */
    private List<Service> enteredServices() {
        List<Service> services = new ArrayList<>();
        for (Node row : serviceRows.getChildren()) {
            Service service = ((ServiceRow) row).service();
            if (!service.getText().isBlank()) {
                services.add(service);
            }
        }
        return services;
    }

    private void edit(Customer customer) {
        current = customer;
        form.load(customer);
        emailForm.load(customer);
        serviceRows.getChildren().clear();
        customer.getServices().forEach(this::addServiceRow);
    }

    private void save() {
        form.store(current);
        emailForm.store(current);
        current.setServices(enteredServices());
        if (current.getName().isBlank()) {
            Dialogs.error(Messages.get("customers.notSaved"), Messages.get("message.nameMissing"));
            return;
        }
        context().store().save(context().company(), current);
        refresh();
    }

    private void delete() {
        if (current.getId().isBlank()) {
            refresh();
            return;
        }
        if (!Dialogs.confirm(Messages.get("customers.deleteTitle"),
                Messages.get("customers.deleteDetail", current.getName()))) {
            return;
        }
        try {
            context().store().delete(context().company(), current);
        } catch (IOException e) {
            Dialogs.error(Messages.get("customers.deleteFailed"), e);
        }
        current = null;
        refresh();
    }
}
