package einvoice.cli;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Plain text table with left-aligned columns, wide enough for their longest cell. */
final class Table {
    private static final String GAP = "  ";

    private final List<List<String>> rows = new ArrayList<>();

    /**
     * Adds a row. The first row added is printed as the heading.
     *
     * @param cells cell texts; null is printed as an empty cell
     * @return this table
     */
    Table row(String... cells) {
        rows.add(Arrays.stream(cells).map(cell -> cell == null ? "" : cell).toList());
        return this;
    }

    /**
     * Prints the table, one line per row, without trailing spaces.
     *
     * @param out target of the output
     */
    void print(PrintStream out) {
        int columns = rows.stream().mapToInt(List::size).max().orElse(0);
        int[] widths = new int[columns];
        for (List<String> row : rows) {
            for (int column = 0; column < row.size(); column++) {
                widths[column] = Math.max(widths[column], row.get(column).length());
            }
        }
        for (List<String> row : rows) {
            StringBuilder line = new StringBuilder();
            for (int column = 0; column < row.size(); column++) {
                if (column > 0) {
                    line.append(GAP);
                }
                line.append(row.get(column)).append(" ".repeat(widths[column] - row.get(column).length()));
            }
            out.println(line.toString().stripTrailing());
        }
    }
}
