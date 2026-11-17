package einvoice.core;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Reads and prints numbers in German notation. */
public final class Numbers {
    private static final DecimalFormatSymbols DE = DecimalFormatSymbols.getInstance(Locale.GERMANY);

    private Numbers() {
    }

    /**
     * Parses a number in German or English notation: "6,75", "1.234,56", "6.75" or "1,234.56". With both
     * separators the last one is the decimal separator; a single comma is always a decimal comma.
     *
     * @param text user input, may be null
     * @return the parsed number, or null if the text is not a number
     */
    public static BigDecimal parse(String text) {
        if (text == null) {
            return null;
        }
        String t = text.replace("€", "").replace(" ", "").trim().replace(" ", "");
        if (t.isEmpty()) {
            return null;
        }
        boolean commaIsDecimal = t.lastIndexOf(',') > t.lastIndexOf('.');
        if (commaIsDecimal) {
            t = t.replace(".", "").replace(',', '.');
        } else {
            t = t.replace(",", "");
        }
        try {
            return new BigDecimal(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Formats an amount with two decimals and its currency.
     *
     * @param value    amount, may be null
     * @param currency ISO 4217 code of the currency
     * @return the amount followed by the euro sign for euro, or by the currency code otherwise
     */
    public static String money(BigDecimal value, String currency) {
        return amount(value) + "\u00A0" + ("EUR".equals(currency) ? "€" : currency);
    }

    /**
     * Formats an amount with two decimals.
     *
     * @param value amount, may be null
     * @return for example "1.234,56", or an empty text for null
     */
    public static String amount(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return new DecimalFormat("#,##0.00", DE).format(value.setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * Formats a quantity without needless decimals.
     *
     * @param value quantity, may be null
     * @return for example "6,75" or "3", or an empty text for null
     */
    public static String quantity(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return new DecimalFormat("#,##0.####", DE).format(value);
    }
}
