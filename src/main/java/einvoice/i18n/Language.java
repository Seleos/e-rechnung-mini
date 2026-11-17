package einvoice.i18n;

import java.util.Locale;

/**
 * Languages of the user interface and of invoices. To add one, add a constant here and the two files
 * {@code messages_<code>.properties} (user interface) and {@code invoice_<code>.properties} (invoice
 * texts) next to the existing ones.
 */
public enum Language {
    GERMAN("de", "Deutsch"),
    ENGLISH("en", "English");

    private final String code;
    private final String displayName;

    Language(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    /**
     * Finds the language for a stored code.
     *
     * @param code ISO 639 language code, may be null or unknown
     * @return the matching language, or the system default if there is none
     */
    public static Language ofCode(String code) {
        for (Language language : values()) {
            if (language.code.equalsIgnoreCase(code)) {
                return language;
            }
        }
        return systemDefault();
    }

    /**
     * Chooses the language for a fresh installation.
     *
     * @return the language of the operating system if supported, otherwise English
     */
    public static Language systemDefault() {
        String systemCode = Locale.getDefault().getLanguage();
        for (Language language : values()) {
            if (language.code.equals(systemCode)) {
                return language;
            }
        }
        return ENGLISH;
    }

    public String getCode() {
        return code;
    }

    /**
     * Returns the locale used to load the texts of this language.
     *
     * @return locale with the language code only
     */
    public Locale locale() {
        return Locale.of(code);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
