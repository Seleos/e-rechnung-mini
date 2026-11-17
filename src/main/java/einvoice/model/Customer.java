package einvoice.model;

import einvoice.i18n.Language;
import einvoice.i18n.Messages;

import java.util.ArrayList;
import java.util.List;

/** Invoice recipient. */
public class Customer {
    /** Currency used unless another one is set for a customer. */
    public static final String DEFAULT_CURRENCY = "EUR";

    private String id = "";
    private String name = "";
    private String addressExtra = "";
    private String street = "";
    private String zip = "";
    private String city = "";
    private String country = "DE";
    private String vatId = "";
    private TaxCase taxCase = TaxCase.DOMESTIC_19;
    private String email = "";
    /** Buyer reference, in Germany the "Leitweg-ID" of public authorities. */
    private String buyerReference = "";
    /** Currency of the invoices for this customer as ISO 4217 code. */
    private String currency = DEFAULT_CURRENCY;
    /** Language the invoices for this customer are written in. */
    private Language invoiceLanguage = Language.GERMAN;
    /** Subject of the e-mail sent with an invoice; may contain placeholders. Empty = standard text. */
    private String emailSubject = "";
    /** Text of the e-mail sent with an invoice; may contain placeholders. Empty = standard text. */
    private String emailBody = "";
    /** Services offered to this customer, each with the rate agreed with this customer. */
    private List<Service> services = new ArrayList<>();

    /**
     * Creates an independent copy, used to freeze the customer data inside an invoice.
     *
     * @return a new customer with the same values
     */
    public Customer copy() {
        Customer copy = new Customer();
        copy.id = id;
        copy.name = name;
        copy.addressExtra = addressExtra;
        copy.street = street;
        copy.zip = zip;
        copy.city = city;
        copy.country = country;
        copy.vatId = vatId;
        copy.taxCase = taxCase;
        copy.email = email;
        copy.buyerReference = buyerReference;
        copy.currency = currency;
        copy.invoiceLanguage = invoiceLanguage;
        copy.emailSubject = emailSubject;
        copy.emailBody = emailBody;
        for (Service service : services) {
            Service serviceCopy = new Service(service.getText(), service.getUnit(), service.getUnitPrice());
            serviceCopy.setIncludedByDefault(service.isIncludedByDefault());
            copy.services.add(serviceCopy);
        }
        return copy;
    }

    @Override
    public String toString() {
        return name == null || name.isBlank() ? Messages.get("customers.new") : name;
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

    public String getAddressExtra() {
        return addressExtra;
    }

    public void setAddressExtra(String addressExtra) {
        this.addressExtra = addressExtra;
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

    public String getVatId() {
        return vatId;
    }

    public void setVatId(String vatId) {
        this.vatId = vatId;
    }

    public TaxCase getTaxCase() {
        return taxCase;
    }

    public void setTaxCase(TaxCase taxCase) {
        this.taxCase = taxCase;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Returns the buyer reference, in Germany the "Leitweg-ID" of public authorities.
     *
     * @return the reference, or an empty text
     */
    public String getBuyerReference() {
        return buyerReference;
    }

    public void setBuyerReference(String buyerReference) {
        this.buyerReference = buyerReference;
    }

    /**
     * Returns the services offered to this customer, each with the rate agreed with this customer.
     *
     * @return the modifiable list of services
     */
    public List<Service> getServices() {
        return services;
    }

    public void setServices(List<Service> services) {
        this.services = services;
    }

    /**
     * Returns the currency of the invoices for this customer.
     *
     * @return the ISO 4217 code in upper case, euro for customers stored before the setting existed
     */
    public String getCurrency() {
        return currency == null || currency.isBlank() ? DEFAULT_CURRENCY : currency.trim().toUpperCase();
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    /**
     * Returns the language the invoices for this customer are written in.
     *
     * @return the invoice language, German for customers stored before the setting existed
     */
    public Language getInvoiceLanguage() {
        return invoiceLanguage == null ? Language.GERMAN : invoiceLanguage;
    }

    public void setInvoiceLanguage(Language invoiceLanguage) {
        this.invoiceLanguage = invoiceLanguage;
    }

    public String getEmailSubject() {
        return emailSubject;
    }

    public void setEmailSubject(String emailSubject) {
        this.emailSubject = emailSubject;
    }

    public String getEmailBody() {
        return emailBody;
    }

    public void setEmailBody(String emailBody) {
        this.emailBody = emailBody;
    }
}
