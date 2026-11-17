package einvoice.ui;

import einvoice.i18n.Language;
import einvoice.i18n.Messages;
import einvoice.model.Company;
import einvoice.service.InvoiceService;
import einvoice.storage.AppSettings;
import einvoice.storage.DataStore;
import einvoice.storage.SampleData;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import javafx.application.HostServices;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/** Shared state of the user interface: data folder, companies and the currently selected company. */
final class AppContext {
    private final AppSettings settings;
    private final HostServices hostServices;
    private final ObservableList<Company> companies = FXCollections.observableArrayList();
    private final ObjectProperty<Company> company = new SimpleObjectProperty<>();
    private DataStore store;
    private InvoiceService invoiceService;
    private Runnable languageChangeHandler = () -> { };

    AppContext(AppSettings settings, HostServices hostServices) {
        this.settings = settings;
        this.hostServices = hostServices;
        Messages.setLanguage(settings.language());
        reload();
    }

    /** Re-reads the data folder. Keeps the selected company if it still exists. */
    void reload() {
        store = new DataStore(settings.dataDir());
        invoiceService = new InvoiceService(store);
        List<Company> loaded = store.companies();
        if (loaded.isEmpty()) {
            loaded = List.of(createSampleCompany());
        }
        String selectedId = company.get() == null ? "" : company.get().getId();
        companies.setAll(loaded);
        company.set(loaded.stream()
                .filter(candidate -> candidate.getId().equals(selectedId))
                .findFirst()
                .orElse(loaded.get(0)));
    }

    /** Switches to another data folder and loads it. */
    void changeDataDir(Path dataDir) {
        Path previous = settings.dataDir();
        settings.setDataDir(dataDir);
        company.set(null);
        try {
            reload();
        } catch (RuntimeException e) {
            // A folder that cannot be used must not be remembered, or the app would not start again.
            settings.setDataDir(previous);
            reload();
            throw e;
        }
    }

    /**
     * Returns a message naming the data files that were skipped because they are damaged, or an empty
     * text. Each damaged file is reported once.
     */
    String takeStorageWarning() {
        List<Path> files = store.takeUnreadableFiles();
        if (files.isEmpty()) {
            return "";
        }
        return files.stream().map(Path::toString).collect(Collectors.joining("\n"));
    }

    /** Switches the language of the user interface and lets the window rebuild itself. */
    void changeLanguage(Language language) {
        if (language == null || language == Messages.language()) {
            return;
        }
        settings.setLanguage(language);
        Messages.setLanguage(language);
        languageChangeHandler.run();
    }

    /** Registers what has to happen after the language changed. */
    void onLanguageChange(Runnable handler) {
        languageChangeHandler = handler;
    }

    /** Opens a file or folder with the program the operating system assigns to it. */
    void open(Path path) {
        hostServices.showDocument(path.toUri().toString());
    }

    /** Opens the folder that contains the given file. */
    void showInFolder(Path file) {
        Path folder = file.toAbsolutePath().getParent();
        if (folder != null) {
            open(folder);
        }
    }

    DataStore store() {
        return store;
    }

    InvoiceService invoiceService() {
        return invoiceService;
    }

    ObservableList<Company> companies() {
        return companies;
    }

    ObjectProperty<Company> companyProperty() {
        return company;
    }

    Company company() {
        return company.get();
    }

    private Company createSampleCompany() {
        try {
            return SampleData.create(store);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
