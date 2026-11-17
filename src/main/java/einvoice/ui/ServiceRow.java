package einvoice.ui;

import atlantafx.base.theme.Styles;
import einvoice.core.Numbers;
import einvoice.core.Units;
import einvoice.i18n.Messages;
import einvoice.model.Service;

import java.math.BigDecimal;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/** One editable service of a customer: name, unit and the rate agreed with this customer. */
final class ServiceRow extends HBox {
    static final double UNIT_WIDTH = 110;
    static final double PRICE_WIDTH = 120;
    static final double DEFAULT_WIDTH = 84;
    static final double REMOVE_WIDTH = 36;

    private final TextField text = new TextField();
    private final ComboBox<String> unit = new ComboBox<>();
    private final TextField unitPrice = new TextField();
    private final CheckBox includedByDefault = new CheckBox();

    ServiceRow(Service service, Runnable onRemove) {
        text.setPromptText(Messages.get("service.namePrompt"));
        text.setText(service.getText());
        HBox.setHgrow(text, Priority.ALWAYS);

        unit.getItems().setAll(Units.all());
        unit.setValue(service.getUnit());
        unit.setMinWidth(UNIT_WIDTH);
        unit.setMaxWidth(UNIT_WIDTH);

        unitPrice.setText(Numbers.amount(service.getUnitPrice()));
        unitPrice.setAlignment(Pos.CENTER_RIGHT);
        unitPrice.setMinWidth(PRICE_WIDTH);
        unitPrice.setMaxWidth(PRICE_WIDTH);

        includedByDefault.setSelected(service.isIncludedByDefault());
        includedByDefault.setAccessibleText(Messages.get("customers.serviceDefault"));
        HBox defaultColumn = new HBox(includedByDefault);
        defaultColumn.setAlignment(Pos.CENTER);
        defaultColumn.setMinWidth(DEFAULT_WIDTH);

        Button remove = new Button("✕");
        remove.getStyleClass().add(Styles.FLAT);
        remove.setMinWidth(REMOVE_WIDTH);
        remove.setAccessibleText(Messages.get("service.remove"));
        remove.setOnAction(event -> onRemove.run());

        setSpacing(8);
        setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(text, unit, unitPrice, defaultColumn, remove);
    }

    /** Current content of the row; a price that is not a number counts as zero. */
    Service service() {
        BigDecimal price = Numbers.parse(unitPrice.getText());
        Service service = new Service(text.getText() == null ? "" : text.getText().trim(),
                unit.getValue() == null ? "" : unit.getValue(), price == null ? BigDecimal.ZERO : price);
        service.setIncludedByDefault(includedByDefault.isSelected());
        return service;
    }
}
