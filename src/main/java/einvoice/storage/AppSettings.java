package einvoice.storage;

import einvoice.AppInfo;
import einvoice.i18n.Language;

import java.nio.file.Path;
import java.util.prefs.Preferences;

/** Settings of this installation that do not belong into the shared data folder. */
public final class AppSettings {
    /** System property that overrides the stored data folder, used for tests and trial runs. */
    public static final String DATA_DIR_PROPERTY = "einvoice.dataDir";

    /** System property that overrides the stored language (a language code), used for tests and trial runs. */
    public static final String LANGUAGE_PROPERTY = "einvoice.language";

    private static final String KEY_DATA_DIR = "dataDir";
    private static final String KEY_LANGUAGE = "language";
    private final Preferences preferences = Preferences.userRoot().node(AppInfo.id());

    /**
     * Returns the folder that holds companies, customers and invoice records.
     *
     * @return the configured data folder, or a default inside the user's documents
     */
    public Path dataDir() {
        String override = System.getProperty(DATA_DIR_PROPERTY);
        if (override != null && !override.isBlank()) {
            return Path.of(override);
        }
        String stored = preferences.get(KEY_DATA_DIR, "");
        return stored.isBlank() ? defaultDataDir() : Path.of(stored);
    }

    /**
     * Remembers a new data folder for this installation.
     *
     * @param dataDir folder chosen by the user
     */
    public void setDataDir(Path dataDir) {
        preferences.put(KEY_DATA_DIR, dataDir.toAbsolutePath().toString());
    }

    /**
     * Returns the language of the user interface.
     *
     * @return the stored language, or the system default on a fresh installation
     */
    public Language language() {
        String override = System.getProperty(LANGUAGE_PROPERTY);
        if (override != null && !override.isBlank()) {
            return Language.ofCode(override);
        }
        return Language.ofCode(preferences.get(KEY_LANGUAGE, ""));
    }

    /**
     * Remembers the language of the user interface for this installation.
     *
     * @param language language chosen by the user
     */
    public void setLanguage(Language language) {
        preferences.put(KEY_LANGUAGE, language.getCode());
    }

    private static Path defaultDataDir() {
        return Path.of(System.getProperty("user.home"), "Documents", AppInfo.name());
    }
}
