package einvoice.ui;

import atlantafx.base.theme.Styles;
import einvoice.core.LogoImage;
import einvoice.core.Placeholders;
import einvoice.i18n.Language;
import einvoice.i18n.Messages;
import einvoice.model.Company;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

/** Page for the settings of the selected company: master data, logo, template, numbers and folders. */
final class SettingsPage extends Page {
    private static final int LOGO_PREVIEW_HEIGHT = 64;

    private final Form<Company> masterData = new Form<>(3);
    private final Form<Company> legal = new Form<>(3);
    private final Form<Company> bank = new Form<>(3);
    private final Form<Company> template = new Form<>(3);
    private final Form<Company> folders = new Form<>(1);
    private final CheckBox smallBusiness = new CheckBox(Messages.get("settings.smallBusiness"));
    private final Spinner<Integer> numberDigits = new Spinner<>(1, 10, 5);
    private final Spinner<Integer> paymentDays = new Spinner<>(0, 365, 30);
    private final Label logoName = new Label();
    private final ImageView logoPreview = new ImageView();
    private final Label dataDir = new Label();
    private final Label templateState = new Label();
    private TextField outputDir;

    SettingsPage(AppContext context) {
        super(context);
        buildForms();

        Button addCompany = new Button(Messages.get("settings.addCompany"));
        addCompany.setOnAction(event -> addCompany());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(12, title(Messages.get("nav.settings")), spacer, addCompany);
        header.setAlignment(Pos.CENTER_LEFT);

        TabPane tabs = new TabPane(
                tab(Messages.get("settings.tab.company"), logoSection(),
                        section(Messages.get("settings.masterData"), masterData.node()),
                        section(Messages.get("settings.legal"), legal.node(), smallBusiness,
                                hint(Messages.get("settings.smallBusinessHint"))),
                        section(Messages.get("settings.bank"), bank.node(),
                                hint(Messages.get("settings.bankHint")))),
                tab(Messages.get("settings.tab.template"),
                        section(Messages.get("settings.numbersAndTexts"), template.node(),
                                hint(Messages.get("placeholders.hint", String.join(", ", Placeholders.NAMES)))),
                        templateSection()),
                tab(Messages.get("settings.tab.general"), languageSection(),
                        section(Messages.get("settings.outputDir"), folders.node(), outputDirButtons()),
                        dataDirSection()));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Button save = new Button(Messages.get("action.save"));
        save.getStyleClass().add(Styles.ACCENT);
        save.setOnAction(event -> save());
        HBox actions = new HBox(save);
        actions.setAlignment(Pos.CENTER_RIGHT);

        getChildren().addAll(header, tabs, actions);
    }

    @Override
    void refresh() {
        Company company = context().company();
        for (Form<Company> form : forms()) {
            form.load(company);
        }
        smallBusiness.setSelected(company.isSmallBusiness());
        logoName.setText(company.getLogo().isBlank() ? Messages.get("settings.noLogo") : company.getLogo());
        showLogoPreview(company);
        dataDir.setText(context().store().root().toString());
        templateState.setText(context().store().templateFile(company) == null
                ? Messages.get("settings.defaultLayout")
                : Messages.get("settings.customLayout"));
    }

    private void buildForms() {
        masterData.wideText(Messages.get("settings.companyName"), Company::getName, Company::setName);
        masterData.text(Messages.get("field.street"), Company::getStreet, Company::setStreet);
        masterData.text(Messages.get("field.zip"), Company::getZip, Company::setZip);
        masterData.text(Messages.get("field.city"), Company::getCity, Company::setCity);
        masterData.text(Messages.get("settings.contact"), Company::getContact, Company::setContact);
        masterData.text(Messages.get("settings.phone"), Company::getPhone, Company::setPhone);
        masterData.text(Messages.get("settings.email"), Company::getEmail, Company::setEmail);
        masterData.text(Messages.get("settings.web"), Company::getWeb, Company::setWeb);

        legal.text(Messages.get("settings.managingDirector"),
                Company::getManagingDirector, Company::setManagingDirector);
        legal.text(Messages.get("settings.registerCourt"), Company::getRegisterCourt, Company::setRegisterCourt);
        legal.text(Messages.get("settings.registerNumber"), Company::getRegisterNumber, Company::setRegisterNumber);
        legal.text(Messages.get("field.vatId"), Company::getVatId, Company::setVatId);
        legal.text(Messages.get("settings.taxNumber"), Company::getTaxNumber, Company::setTaxNumber);

        bank.text(Messages.get("settings.bankName"), Company::getBankName, Company::setBankName);
        bank.text("IBAN", Company::getIban, Company::setIban);
        bank.text("BIC", Company::getBic, Company::setBic);

        numberDigits.setMaxWidth(Double.MAX_VALUE);
        paymentDays.setMaxWidth(Double.MAX_VALUE);
        paymentDays.setEditable(true);
        template.text(Messages.get("settings.numberPrefix"), Company::getNumberPrefix, Company::setNumberPrefix);
        template.custom(Messages.get("settings.numberDigits"), numberDigits, 1,
                company -> numberDigits.getValueFactory().setValue(company.getNumberDigits()),
                company -> company.setNumberDigits(numberDigits.getValue()));
        template.custom(Messages.get("settings.paymentDays"), paymentDays, 1,
                company -> paymentDays.getValueFactory().setValue(company.getPaymentDays()),
                company -> company.setPaymentDays(paymentDays.getValue()));
        template.textArea(Messages.get("settings.paymentText"), Company::getPaymentText, Company::setPaymentText);
        template.textArea(Messages.get("settings.paymentTextEnglish"),
                Company::getPaymentTextEnglish, Company::setPaymentTextEnglish);
        template.wideText(Messages.get("settings.footerExtra"), Company::getFooterExtra, Company::setFooterExtra);

        outputDir = folders.wideText(Messages.get("settings.outputDirField"),
                Company::getOutputDir, Company::setOutputDir);
    }

    private Form<Company>[] forms() {
        @SuppressWarnings("unchecked")
        Form<Company>[] forms = new Form[] {masterData, legal, bank, template, folders};
        return forms;
    }

    private void save() {
        Company company = context().company();
        for (Form<Company> form : forms()) {
            form.store(company);
        }
        company.setSmallBusiness(smallBusiness.isSelected());
        if (company.getName().isBlank()) {
            Dialogs.error(Messages.get("settings.companyNotSaved"), Messages.get("message.nameMissing"));
            return;
        }
        context().store().save(company);
        context().reload();
    }

    private void addCompany() {
        Company company = new Company();
        company.setName(Messages.get("settings.newCompany"));
        company.useStandardPaymentTexts();
        context().store().save(company);
        context().reload();
        context().companies().stream()
                .filter(candidate -> candidate.getId().equals(company.getId()))
                .findFirst()
                .ifPresent(context().companyProperty()::set);
    }

    private Node languageSection() {
        ComboBox<Language> language = new ComboBox<>();
        language.getItems().setAll(Language.values());
        language.setValue(Messages.language());
        language.setOnAction(event -> context().changeLanguage(language.getValue()));
        return section(Messages.get("settings.language"), language, hint(Messages.get("settings.languageHint")));
    }

    private Node logoSection() {
        Button choose = new Button(Messages.get("settings.chooseLogo"));
        choose.setOnAction(event -> chooseLogo());
        Button remove = new Button(Messages.get("action.remove"));
        remove.getStyleClass().add(Styles.FLAT);
        remove.setOnAction(event -> removeLogo());
        logoPreview.setPreserveRatio(true);
        logoPreview.setFitHeight(LOGO_PREVIEW_HEIGHT);
        return section(Messages.get("settings.logo"), logoPreview, logoName, hint(Messages.get("settings.logoHint")),
                new HBox(8, choose, remove));
    }

    /** Shows the logo as it will appear on the invoice; a file that cannot be read is simply not shown. */
    private void showLogoPreview(Company company) {
        Path file = context().store().logoFile(company);
        logoPreview.setImage(null);
        logoPreview.setManaged(file != null);
        if (file == null) {
            return;
        }
        try {
            // Rendered at twice the display height so the preview stays sharp on high-resolution screens.
            logoPreview.setImage(SwingFXUtils.toFXImage(LogoImage.load(file, 2 * LOGO_PREVIEW_HEIGHT), null));
        } catch (IOException e) {
            logoPreview.setManaged(false);
        }
    }

    private void chooseLogo() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(Messages.get("settings.chooseLogo"));
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(Messages.get("settings.images"), "*.svg", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            context().store().setLogo(context().company(), file.toPath());
        } catch (IOException e) {
            Dialogs.error(Messages.get("settings.logoFailed"), e);
        }
        refresh();
    }

    private void removeLogo() {
        try {
            context().store().removeLogo(context().company());
            context().store().save(context().company());
        } catch (IOException e) {
            Dialogs.error(Messages.get("settings.logoRemoveFailed"), e);
        }
        refresh();
    }

    private Node templateSection() {
        Button export = new Button(Messages.get("settings.exportTemplate"));
        export.setOnAction(event -> exportTemplate());
        return section(Messages.get("settings.layout"), templateState,
                hint(Messages.get("settings.layoutHint")),
                export);
    }

    private void exportTemplate() {
        try {
            Path file = context().store().exportTemplate(context().company());
            context().showInFolder(file);
        } catch (IOException e) {
            Dialogs.error(Messages.get("settings.templateFailed"), e);
        }
        refresh();
    }

    private Node outputDirButtons() {
        Button choose = new Button(Messages.get("action.chooseFolder"));
        choose.setOnAction(event -> {
            File folder = new DirectoryChooser().showDialog(getScene().getWindow());
            if (folder != null) {
                outputDir.setText(folder.getAbsolutePath());
            }
        });
        return choose;
    }

    private Node dataDirSection() {
        Button choose = new Button(Messages.get("settings.chooseDataDir"));
        choose.setOnAction(event -> {
            File folder = new DirectoryChooser().showDialog(getScene().getWindow());
            if (folder != null) {
                context().changeDataDir(folder.toPath());
            }
        });
        return section(Messages.get("settings.dataDir"), dataDir,
                hint(Messages.get("settings.dataDirHint")),
                choose);
    }

    private static Tab tab(String title, Node... sections) {
        VBox content = new VBox(16, sections);
        content.setPadding(new Insets(16, 0, 0, 0));
        return new Tab(title, content);
    }

    private static VBox section(String title, Node... content) {
        Label heading = new Label(title);
        heading.getStyleClass().add(Styles.TITLE_4);
        VBox section = new VBox(12, heading);
        section.getChildren().addAll(content);
        section.getStyleClass().add("card");
        return section;
    }
}
