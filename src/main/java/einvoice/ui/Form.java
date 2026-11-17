package einvoice.ui;

import atlantafx.base.theme.Styles;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * Grid of labelled input fields bound to the properties of one object.
 *
 * @param <T> type of the edited object
 */
final class Form<T> {
    private static final double GAP = 16;

    private final GridPane grid = new GridPane();
    private final List<Consumer<T>> loaders = new ArrayList<>();
    private final List<Consumer<T>> writers = new ArrayList<>();
    private final int columns;
    private int nextCell;

    Form(int columns) {
        this.columns = columns;
        grid.setHgap(GAP);
        grid.setVgap(GAP);
        for (int column = 0; column < columns; column++) {
            ColumnConstraints constraints = new ColumnConstraints();
            constraints.setPercentWidth(100.0 / columns);
            grid.getColumnConstraints().add(constraints);
        }
    }

    /** Adds a single-line text field that takes one column. */
    TextField text(String label, Function<T, String> getter, BiConsumer<T, String> setter) {
        TextField field = new TextField();
        bindText(field, getter, setter);
        add(label, field, 1);
        return field;
    }

    /** Adds a single-line text field that spans the whole row. */
    TextField wideText(String label, Function<T, String> getter, BiConsumer<T, String> setter) {
        TextField field = new TextField();
        bindText(field, getter, setter);
        add(label, field, columns);
        return field;
    }

    /** Adds a multi-line text field that spans the whole row. */
    TextArea textArea(String label, Function<T, String> getter, BiConsumer<T, String> setter) {
        TextArea area = new TextArea();
        area.setWrapText(true);
        area.setPrefRowCount(3);
        bindText(area, getter, setter);
        add(label, area, columns);
        return area;
    }

    /** Adds any control; loading and storing its value is up to the given functions. */
    void custom(String label, Node control, int span, Consumer<T> loader, Consumer<T> writer) {
        loaders.add(loader);
        writers.add(writer);
        add(label, control, span);
    }

    /** Fills all fields from the object. */
    void load(T source) {
        loaders.forEach(loader -> loader.accept(source));
    }

    /** Writes all field values into the object. */
    void store(T target) {
        writers.forEach(writer -> writer.accept(target));
    }

    GridPane node() {
        return grid;
    }

    private void bindText(TextInputControl control, Function<T, String> getter, BiConsumer<T, String> setter) {
        loaders.add(source -> {
            String value = getter.apply(source);
            control.setText(value == null ? "" : value);
        });
        writers.add(target -> setter.accept(target, control.getText() == null ? "" : control.getText().trim()));
    }

    private void add(String labelText, Node control, int span) {
        int column = nextCell % columns;
        if (column + span > columns) {
            nextCell += columns - column;
            column = 0;
        }
        Label label = new Label(labelText);
        label.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.TEXT_MUTED);
        VBox cell = new VBox(6, label, control);
        grid.add(cell, column, nextCell / columns, span, 1);
        nextCell += span;
    }
}
