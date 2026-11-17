package einvoice;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * Name and version of the application. The values come from {@code pom.xml} and are written into
 * {@code app.properties} by the build, so renaming the product means changing the POM only.
 */
public final class AppInfo {
    private static final String UNRESOLVED_PREFIX = "${";
    private static final Properties VALUES = load();

    private AppInfo() {
    }

    /**
     * Returns the product name shown in the window title and recorded in generated files.
     *
     * @return the display name
     */
    public static String name() {
        return value("name", "eInvoice");
    }

    /**
     * Returns the technical identifier used for settings of this installation.
     *
     * @return an identifier without spaces
     */
    public static String id() {
        return value("id", "einvoice");
    }

    /**
     * Returns the version of this build.
     *
     * @return the version number
     */
    public static String version() {
        return value("version", "dev");
    }

    /** The fallback applies when the app runs without a Maven build, for example straight from an IDE. */
    private static String value(String key, String fallback) {
        String value = VALUES.getProperty(key, "");
        return value.isBlank() || value.startsWith(UNRESOLVED_PREFIX) ? fallback : value;
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream in = AppInfo.class.getResourceAsStream("/einvoice/app.properties")) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return properties;
    }
}
