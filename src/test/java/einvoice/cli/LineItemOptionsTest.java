package einvoice.cli;

import einvoice.model.LineItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests changing and replacing the line items of a draft. */
class LineItemOptionsTest {
    private static final List<LineItem> LINES = List.of(
            new LineItem(null, "h", "Wartung", new BigDecimal("70.00")),
            new LineItem(BigDecimal.ONE, "pauschal", "Hosting", new BigDecimal("49.00")));

    @Test
    void valuesChangeTheLinesInOrderAndLeaveTheRest() throws CommandFailure {
        List<LineItem> lines = LineItemOptions.apply(arguments(
                "--qty", "6,75", "--qty", "2", "--price", "1.600,00", "--text", "Wartung Website\\n{period}",
                "--unit", "Tag"), LINES);

        assertEquals(new BigDecimal("6.75"), lines.get(0).getQuantity());
        assertEquals(new BigDecimal("1600.00"), lines.get(0).getUnitPrice());
        assertEquals("Wartung Website\n{period}", lines.get(0).getText());
        assertEquals("Tag", lines.get(0).getUnit());
        assertEquals(new BigDecimal("2"), lines.get(1).getQuantity());
        assertEquals("Hosting", lines.get(1).getText());
        assertEquals("pauschal", lines.get(1).getUnit());
    }

    @Test
    void givenLinesAreNotChanged() throws CommandFailure {
        LineItemOptions.apply(arguments("--qty", "5"), LINES);

        assertNull(LINES.get(0).getQuantity());
    }

    @Test
    void itemsReplaceAllLinesAndTheTextMayContainTheSeparator() throws CommandFailure {
        List<LineItem> lines = LineItemOptions.apply(arguments(
                "--item", "Beratung; Konzept;2;h;100", "--item", "Reise;1;pauschal;50,5"), LINES);

        assertEquals(2, lines.size());
        assertEquals("Beratung; Konzept", lines.get(0).getText());
        assertEquals(new BigDecimal("2"), lines.get(0).getQuantity());
        assertEquals("h", lines.get(0).getUnit());
        assertEquals(new BigDecimal("100"), lines.get(0).getUnitPrice());
        assertEquals(new BigDecimal("50.5"), lines.get(1).getUnitPrice());
    }

    @Test
    void rejectsWrongValues() {
        assertUsage("--qty", "viel");
        assertUsage("--price", "");
        assertUsage("--unit", "Liter");
        assertUsage("--qty", "1", "--qty", "2", "--qty", "3");
        assertUsage("--item", "Beratung;2;h");
        assertUsage("--item", "Beratung;2;h;100", "--price", "90");
    }

    private static void assertUsage(String... options) {
        CommandFailure failure = assertThrows(CommandFailure.class,
                () -> LineItemOptions.apply(arguments(options), LINES));
        assertEquals(CommandLine.USAGE, failure.exitCode());
    }

    private static Arguments arguments(String... options) throws CommandFailure {
        String[] args = new String[options.length + 1];
        args[0] = "create";
        System.arraycopy(options, 0, args, 1, options.length);
        return Arguments.parse(args, Set.of());
    }
}
