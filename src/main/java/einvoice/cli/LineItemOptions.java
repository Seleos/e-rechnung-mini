package einvoice.cli;

import einvoice.core.Numbers;
import einvoice.core.Units;
import einvoice.i18n.Messages;
import einvoice.model.LineItem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Options that change the line items of a draft. {@code --qty}, {@code --price}, {@code --text} and
 * {@code --unit} change the existing lines in order: the first value goes to line 1, the second to line 2.
 * {@code --item "text;quantity;unit;price"} replaces all lines instead.
 */
final class LineItemOptions {
    static final String QUANTITY = "qty";
    static final String PRICE = "price";
    static final String TEXT = "text";
    static final String UNIT = "unit";
    static final String ITEM = "item";

    /** All option names handled here. */
    static final Set<String> NAMES = Set.of(QUANTITY, PRICE, TEXT, UNIT, ITEM);

    private static final String SEPARATOR = ";";
    private static final int ITEM_FIELDS = 4;
    /** Typed as two characters on the command line, because a shell argument rarely holds a real line break. */
    private static final String LINE_BREAK = "\\n";

    private LineItemOptions() {
    }

    /**
     * Applies the options to the lines of a draft.
     *
     * @param arguments parsed arguments
     * @param items     lines of the draft
     * @return the changed lines; the given list is not modified
     * @throws CommandFailure if values cannot be read, there are more values than lines, or {@code --item} is
     *                        mixed with the options that change single lines
     */
    static List<LineItem> apply(Arguments arguments, List<LineItem> items) throws CommandFailure {
        List<String> replacements = arguments.values(ITEM);
        if (!replacements.isEmpty()) {
            for (String name : List.of(QUANTITY, PRICE, TEXT, UNIT)) {
                if (arguments.has(name)) {
                    throw CommandFailure.usage(Messages.get("cli.error.itemAndChanges", "--" + name));
                }
            }
            List<LineItem> lines = new ArrayList<>();
            for (String replacement : replacements) {
                lines.add(item(replacement));
            }
            return lines;
        }
        List<LineItem> lines = new ArrayList<>();
        for (LineItem item : items) {
            lines.add(new LineItem(item.getQuantity(), item.getUnit(), item.getText(), item.getUnitPrice()));
        }
        change(arguments, QUANTITY, lines, (line, value) -> line.setQuantity(number(QUANTITY, value)));
        change(arguments, PRICE, lines, (line, value) -> line.setUnitPrice(number(PRICE, value)));
        change(arguments, TEXT, lines, (line, value) -> line.setText(text(value)));
        change(arguments, UNIT, lines, (line, value) -> line.setUnit(unit(value)));
        return lines;
    }

    /** Parses "text;quantity;unit;price". The text is split off at the last three separators and may hold more. */
    private static LineItem item(String value) throws CommandFailure {
        String[] fields = new String[ITEM_FIELDS];
        String rest = value;
        for (int field = ITEM_FIELDS - 1; field > 0; field--) {
            int separator = rest.lastIndexOf(SEPARATOR);
            if (separator < 0) {
                throw CommandFailure.usage(Messages.get("cli.error.item", value));
            }
            fields[field] = rest.substring(separator + 1).trim();
            rest = rest.substring(0, separator);
        }
        fields[0] = rest;
        return new LineItem(number(QUANTITY, fields[1]), unit(fields[2]), text(fields[0]), number(PRICE, fields[3]));
    }

    private static void change(Arguments arguments, String name, List<LineItem> lines, Change change)
            throws CommandFailure {
        List<String> values = arguments.values(name);
        if (values.size() > lines.size()) {
            throw CommandFailure.usage(Messages.get("cli.error.tooManyValues", "--" + name, lines.size()));
        }
        for (int index = 0; index < values.size(); index++) {
            change.accept(lines.get(index), values.get(index));
        }
    }

    private static BigDecimal number(String name, String value) throws CommandFailure {
        BigDecimal number = Numbers.parse(value);
        if (number == null) {
            throw CommandFailure.usage(Messages.get("cli.error.number", "--" + name, value));
        }
        return number;
    }

    private static String text(String value) {
        return value.replace(LINE_BREAK, "\n").strip();
    }

    private static String unit(String value) throws CommandFailure {
        String unit = value.trim();
        if (!Units.all().contains(unit)) {
            throw CommandFailure.usage(Messages.get("cli.error.unit", value, String.join(", ", Units.all())));
        }
        return unit;
    }

    /** Like a {@link java.util.function.BiConsumer}, but a value may be rejected. */
    @FunctionalInterface
    private interface Change {
        void accept(LineItem line, String value) throws CommandFailure;
    }
}
