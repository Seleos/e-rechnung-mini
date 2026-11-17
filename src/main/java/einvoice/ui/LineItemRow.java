package einvoice.ui;

import atlantafx.base.theme.Styles;
import einvoice.core.Numbers;
import einvoice.core.Units;
import einvoice.i18n.Messages;
import einvoice.model.LineItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/** One editable invoice line: quantity, unit, text, unit price and the resulting line total. */
final class LineItemRow extends HBox {
    static final String QUANTITY_STYLE_CLASS = "quantity-field";
    static final double POSITION_WIDTH = 24;
    static final double QUANTITY_WIDTH = 72;
    static final double UNIT_WIDTH = 92;
    static final double PRICE_WIDTH = 92;
    static final double TOTAL_WIDTH = 96;
    static final double REMOVE_WIDTH = 36;

    private final Label position = new Label();
    private final TextField quantity = new TextField();
    private final ComboBox<String> unit = new ComboBox<>();
    private final TextArea text = new TextArea();
    private final TextField unitPrice = new TextField();
    private final Label total = new Label();

    LineItemRow(LineItem item, Runnable onChange, Runnable onRemove) {
        position.setMinWidth(POSITION_WIDTH);
        // Number and total are as high as the input fields, so their text sits on the same line.
        position.minHeightProperty().bind(quantity.heightProperty());
        total.minHeightProperty().bind(quantity.heightProperty());
        fixWidth(quantity, QUANTITY_WIDTH);
        quantity.setAlignment(Pos.CENTER_RIGHT);
        quantity.getStyleClass().add(QUANTITY_STYLE_CLASS);
        unit.getItems().setAll(Units.all());
        fixWidth(unit, UNIT_WIDTH);
        text.setPrefRowCount(2);
        text.setWrapText(true);
        text.setPromptText(Messages.get("item.textPrompt"));
        HBox.setHgrow(text, Priority.ALWAYS);
        fixWidth(unitPrice, PRICE_WIDTH);
        unitPrice.setAlignment(Pos.CENTER_RIGHT);
        fixWidth(total, TOTAL_WIDTH);
        total.setAlignment(Pos.CENTER_RIGHT);

        Button remove = new Button("✕");
        remove.getStyleClass().add(Styles.FLAT);
        remove.setMinWidth(REMOVE_WIDTH);
        remove.setAccessibleText(Messages.get("item.remove"));
        remove.setOnAction(event -> onRemove.run());

        quantity.setText(Numbers.quantity(item.getQuantity()));
        unit.setValue(item.getUnit());
        text.setText(item.getText());
        unitPrice.setText(Numbers.amount(item.getUnitPrice()));
        updateTotal();

        Runnable changed = () -> {
            updateTotal();
            onChange.run();
        };
        quantity.textProperty().addListener((property, old, value) -> changed.run());
        unitPrice.textProperty().addListener((property, old, value) -> changed.run());
        text.textProperty().addListener((property, old, value) -> changed.run());
        unit.valueProperty().addListener((property, old, value) -> changed.run());

        setSpacing(8);
        setAlignment(Pos.TOP_LEFT);
        getChildren().addAll(position, quantity, unit, text, unitPrice, total, remove);
    }

    private static void fixWidth(Region control, double width) {
        control.setMinWidth(width);
        control.setPrefWidth(width);
        control.setMaxWidth(width);
    }

    /** Puts the cursor into the quantity field, the one value a pre-filled line still needs. */
    void focusQuantity() {
        quantity.requestFocus();
    }

    void setPosition(int number) {
        position.setText(String.valueOf(number));
    }

    /** Current content of the row; quantity or price are null while the input is not a number. */
    LineItem item() {
        return new LineItem(Numbers.parse(quantity.getText()), unit.getValue() == null ? "" : unit.getValue(),
                text.getText() == null ? "" : text.getText().strip(), Numbers.parse(unitPrice.getText()));
    }

    private void updateTotal() {
        BigDecimal parsedQuantity = Numbers.parse(quantity.getText());
        BigDecimal parsedPrice = Numbers.parse(unitPrice.getText());
        if (parsedQuantity == null || parsedPrice == null) {
            total.setText("");
            return;
        }
        total.setText(Numbers.amount(parsedQuantity.multiply(parsedPrice).setScale(2, RoundingMode.HALF_UP)));
    }
}
