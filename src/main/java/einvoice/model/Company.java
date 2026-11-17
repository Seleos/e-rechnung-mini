package einvoice.model;

import einvoice.i18n.InvoiceTexts;
import einvoice.i18n.Language;

/** Issuing company with everything needed for the invoice, the footer and the number range. */
public class Company {
    private String id = "";
    private String name = "";
    private String street = "";
    private String zip = "";
    private String city = "";
    private String country = "DE";
    private String phone = "";
    private String email = "";
    private String web = "";
    private String contact = "";
    private String managingDirector = "";
    private String registerCourt = "";
    private String registerNumber = "";
    private String vatId = "";
    private String taxNumber = "";
    /** German "Kleinunternehmer" (§ 19 UStG): invoices carry no VAT. */
    private boolean smallBusiness = false;
    private String bankName = "";
    private String iban = "";
    private String bic = "";
    /** Invoice number prefix; {year} is replaced by the invoice year. */
    private String numberPrefix = "RE-{year}-";
    private int numberDigits = 5;
    private int paymentDays = 30;
    private String paymentText = "";
    /** Payment text for invoices written in English. Empty = the German text is used. */
    private String paymentTextEnglish = "";
    private String footerExtra = "";
    /** Target folder for finished invoices; {year} is replaced. Empty = "invoices" inside the company folder. */
    private String outputDir = "";
    /** File name of the logo inside the company folder, empty = no logo. */
    private String logo = "";

    @Override
    public String toString() {
        return name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getWeb() {
        return web;
    }

    public void setWeb(String web) {
        this.web = web;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getManagingDirector() {
        return managingDirector;
    }

    public void setManagingDirector(String managingDirector) {
        this.managingDirector = managingDirector;
    }

    public String getRegisterCourt() {
        return registerCourt;
    }

    public void setRegisterCourt(String registerCourt) {
        this.registerCourt = registerCourt;
    }

    public String getRegisterNumber() {
        return registerNumber;
    }

    public void setRegisterNumber(String registerNumber) {
        this.registerNumber = registerNumber;
    }

    public String getVatId() {
        return vatId;
    }

    public void setVatId(String vatId) {
        this.vatId = vatId;
    }

    public String getTaxNumber() {
        return taxNumber;
    }

    public void setTaxNumber(String taxNumber) {
        this.taxNumber = taxNumber;
    }

    /**
     * Tells whether the company is a German "Kleinunternehmer" (§ 19 UStG) whose invoices carry no VAT.
     *
     * @return true if no VAT is charged on any invoice
     */
    public boolean isSmallBusiness() {
        return smallBusiness;
    }

    public void setSmallBusiness(boolean smallBusiness) {
        this.smallBusiness = smallBusiness;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public String getBic() {
        return bic;
    }

    public void setBic(String bic) {
        this.bic = bic;
    }

    /**
     * Returns the prefix of the invoice numbers; {year} stands for the invoice year.
     *
     * @return the prefix, for example "RE-{year}-"
     */
    public String getNumberPrefix() {
        return numberPrefix;
    }

    public void setNumberPrefix(String numberPrefix) {
        this.numberPrefix = numberPrefix;
    }

    public int getNumberDigits() {
        return numberDigits;
    }

    public void setNumberDigits(int numberDigits) {
        this.numberDigits = numberDigits;
    }

    public int getPaymentDays() {
        return paymentDays;
    }

    public void setPaymentDays(int paymentDays) {
        this.paymentDays = paymentDays;
    }

    public String getPaymentText() {
        return paymentText;
    }

    public void setPaymentText(String paymentText) {
        this.paymentText = paymentText;
    }

    public String getFooterExtra() {
        return footerExtra;
    }

    public void setFooterExtra(String footerExtra) {
        this.footerExtra = footerExtra;
    }

    /**
     * Returns the configured folder for finished invoices; {year} stands for the invoice year.
     *
     * @return the folder, or an empty text for the default inside the company folder
     */
    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    /**
     * Returns the file name of the logo inside the company folder.
     *
     * @return the file name, or an empty text if no logo is set
     */
    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getPaymentTextEnglish() {
        return paymentTextEnglish;
    }

    public void setPaymentTextEnglish(String paymentTextEnglish) {
        this.paymentTextEnglish = paymentTextEnglish;
    }

    /**
     * Fills the texts below the invoice with the standard wording in both invoice languages.
     * Used for new companies; existing texts are replaced.
     */
    public void useStandardPaymentTexts() {
        paymentText = InvoiceTexts.of(Language.GERMAN).get("payment.default");
        paymentTextEnglish = InvoiceTexts.of(Language.ENGLISH).get("payment.default");
    }
}
