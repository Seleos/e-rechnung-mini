package einvoice;

import einvoice.cli.CommandLine;
import einvoice.ui.App;

import javafx.application.Application;

/**
 * Entry point. A separate class is needed because a JavaFX {@link Application} cannot be started
 * directly from the class path. Started with a command such as {@code invoice create} or {@code --help}, it
 * runs the {@link CommandLine} instead of opening the window.
 */
public final class Launcher {
    private Launcher() {
    }

    /**
     * Starts the application.
     *
     * @param args a command with its options for the command line, otherwise passed on to JavaFX
     */
    public static void main(String[] args) {
        // PDF rendering uses Java2D off-screen; without this flag AWT and JavaFX block each other on macOS.
        System.setProperty("java.awt.headless", "true");
        if (CommandLine.isCommand(args)) {
            System.exit(CommandLine.forInstallation().run(args));
        }
        Application.launch(App.class, args);
    }
}
