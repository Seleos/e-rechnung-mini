package einvoice.i18n;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Texts of the user interface in the selected language. All user-facing texts are looked up here by key;
 * the texts themselves live in {@code messages_<code>.properties}.
 */
public final class Messages {
    /** Base name of the resource bundles. */
    public static final String BUNDLE = "einvoice.messages";

    /** Selected language together with its texts; replaced as a whole so readers never see a mix. */
    private static volatile Selection selection = Selection.of(Language.systemDefault());

    private Messages() {
    }

    /**
     * A language and the texts loaded for it.
     *
     * @param language selected language
     * @param texts    texts of that language
     */
    private record Selection(Language language, ResourceBundle texts) {
        static Selection of(Language language) {
            // No fallback to the system locale: a missing text should show up as missing, not in another language.
            return new Selection(language, ResourceBundle.getBundle(BUNDLE, language.locale(),
                    ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES)));
        }
    }

    /**
     * Switches the language of all texts looked up from now on.
     *
     * @param language language to use
     */
    public static void setLanguage(Language language) {
        selection = Selection.of(language);
    }

    /**
     * Returns the language currently in use.
     *
     * @return the selected language
     */
    public static Language language() {
        return selection.language();
    }

    /**
     * Looks up a text.
     *
     * @param key       key of the text in the properties files
     * @param arguments values for the placeholders {0}, {1} ... of the text, if it has any
     * @return the text in the selected language, or the key between exclamation marks if it is missing
     */
    public static String get(String key, Object... arguments) {
        Selection current = selection;
        String pattern;
        try {
            pattern = current.texts().getString(key);
        } catch (MissingResourceException e) {
            return "!" + key + "!";
        }
        if (arguments.length == 0) {
            return pattern;
        }
        return new MessageFormat(pattern, current.language().locale()).format(arguments);
    }
}
