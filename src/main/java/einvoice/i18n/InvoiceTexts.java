package einvoice.i18n;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

/**
 * Texts that end up on an invoice or in the e-mail sent with it, in the language chosen for the customer.
 * This is independent of the language of the user interface; the texts live in
 * {@code invoice_<code>.properties}.
 */
public final class InvoiceTexts {
    /** Base name of the resource bundles. */
    public static final String BUNDLE = "einvoice.invoice";

    private final Language language;
    private final ResourceBundle texts;

    private InvoiceTexts(Language language) {
        this.language = language;
        this.texts = ResourceBundle.getBundle(BUNDLE, language.locale(),
                ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));
    }

    /**
     * Loads the invoice texts of a language.
     *
     * @param language invoice language, null means German
     * @return the texts of that language
     */
    public static InvoiceTexts of(Language language) {
        return new InvoiceTexts(language == null ? Language.GERMAN : language);
    }

    /**
     * Looks up a text.
     *
     * @param key key of the text in the properties files
     * @return the text in the invoice language
     * @throws java.util.MissingResourceException if the key does not exist
     */
    public String get(String key) {
        return texts.getString(key);
    }

    /**
     * Lists the keys that start with one of the given prefixes.
     *
     * @param prefixes key prefixes such as "label."
     * @return the matching keys in alphabetical order
     */
    public Set<String> keys(String... prefixes) {
        Set<String> keys = new TreeSet<>();
        for (String key : texts.keySet()) {
            for (String prefix : prefixes) {
                if (key.startsWith(prefix)) {
                    keys.add(key);
                }
            }
        }
        return keys;
    }

    /**
     * Formats a date the way it is written on an invoice in this language.
     *
     * @param date date to format
     * @return for example "17.11.2026" in German and "17 November 2026" in English
     */
    public String date(LocalDate date) {
        return DateTimeFormatter.ofPattern(get("format.date"), locale()).format(date);
    }

    /**
     * Returns the name of the month of a date.
     *
     * @param date any day of the month
     * @return for example "Oktober" or "October"
     */
    public String month(LocalDate date) {
        return date.getMonth().getDisplayName(TextStyle.FULL, locale());
    }

    /**
     * Describes a service period the way it is printed on the invoice.
     *
     * @param from first day of the period, may be null
     * @param to   last day of the period, may be null
     * @return the month and year for a full month, a single date for one day, otherwise both dates;
     *         empty if a date is missing
     */
    public String period(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            return "";
        }
        boolean fullMonth = from.getDayOfMonth() == 1 && to.equals(from.withDayOfMonth(from.lengthOfMonth()));
        if (fullMonth) {
            return month(from) + " " + from.getYear();
        }
        if (from.equals(to)) {
            return date(from);
        }
        return date(from) + " – " + date(to);
    }

    /**
     * Returns the invoice language.
     *
     * @return the language these texts are written in
     */
    public Language language() {
        return language;
    }

    /**
     * Returns the locale for month and country names on the invoice.
     *
     * @return locale of the invoice language
     */
    public Locale locale() {
        return language.locale();
    }
}
