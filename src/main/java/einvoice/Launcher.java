package einvoice;

import einvoice.ui.App;

import javafx.application.Application;

/**
 * Entry point. A separate class is needed because a JavaFX {@link Application} cannot be started
 * directly from the class path.
 */
public final class Launcher {
    private Launcher() {
    }

    /**
     * Starts the application.
     *
     * @param args command line arguments, passed on to JavaFX
     */
    public static void main(String[] args) {
        // PDF rendering uses Java2D off-screen; without this flag AWT and JavaFX block each other on macOS.
        System.setProperty("java.awt.headless", "true");
        Application.launch(App.class, args);
    }
}
