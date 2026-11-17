package einvoice.ui;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import einvoice.AppInfo;
import einvoice.storage.AppSettings;

import javafx.application.Application;
import javafx.application.ColorScheme;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/** JavaFX application: applies the theme and opens the main window. */
public final class App extends Application {
    private static final double WIDTH = 1440;
    private static final double HEIGHT = 900;
    private static final String[] ICONS = {"/einvoice/icon-small.png", "/einvoice/icon.png"};

    @Override
    public void start(Stage stage) {
        applyTheme(Platform.getPreferences().getColorScheme());
        Platform.getPreferences().colorSchemeProperty().addListener((property, old, scheme) -> applyTheme(scheme));

        AppContext context = new AppContext(new AppSettings(), getHostServices());
        Scene scene = new Scene(new MainWindow(context), WIDTH, HEIGHT);
        // Deferred, because the change is triggered from a control inside the window that gets replaced.
        context.onLanguageChange(() -> Platform.runLater(() -> rebuildWindow(scene, context)));
        scene.getStylesheets().add(App.class.getResource("/einvoice/app.css").toExternalForm());
        stage.setTitle(AppInfo.name());
        for (String icon : ICONS) {
            stage.getIcons().add(new Image(App.class.getResource(icon).toExternalForm()));
        }
        stage.setMinWidth(1200);
        stage.setMinHeight(640);
        stage.setScene(scene);
        stage.show();
    }

    /** Texts are read when a page is built, so a new language needs a new window content. */
    static void rebuildWindow(Scene scene, AppContext context) {
        if (scene.getRoot() instanceof MainWindow oldWindow) {
            oldWindow.dispose();
        }
        MainWindow window = new MainWindow(context);
        scene.setRoot(window);
        window.show(MainWindow.PageId.SETTINGS);
    }

    private static void applyTheme(ColorScheme scheme) {
        boolean dark = scheme == ColorScheme.DARK;
        Application.setUserAgentStylesheet(
                dark ? new PrimerDark().getUserAgentStylesheet() : new PrimerLight().getUserAgentStylesheet());
    }
}
