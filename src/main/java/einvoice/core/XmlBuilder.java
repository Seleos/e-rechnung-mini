package einvoice.core;

import einvoice.i18n.InvoiceTexts;
import einvoice.i18n.Language;
import einvoice.model.Company;
import einvoice.model.Customer;
import einvoice.model.InvoiceData;
import einvoice.model.LineItem;
import org.mustangproject.BankDetails;
import org.mustangproject.Contact;
import org.mustangproject.Invoice;
import org.mustangproject.Item;
import org.mustangproject.LegalOrganisation;
import org.mustangproject.Product;
import org.mustangproject.TradeParty;
import org.mustangproject.ZUGFeRD.Profiles;
import org.mustangproject.ZUGFeRD.TransactionCalculator;
import org.mustangproject.ZUGFeRD.ZUGFeRD2PullProvider;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/** Builds the structured invoice (CII, profile EN 16931) from company and invoice data. */
public final class XmlBuilder {
    /** ZUGFeRD profile used for all invoices. */
    public static final String PROFILE = "EN16931";

    private static final String NOT_SUBJECT_TO_VAT = "O";
    private static final String DEFAULT_COUNTRY = "DE";

    private XmlBuilder() {
    }

    /**
     * Invoice totals, calculated with the same logic as the XML.
     *
     * @param net   sum of all lines without VAT
     * @param tax   VAT amount
     * @param gross amount due
     */
    public record Totals(BigDecimal net, BigDecimal tax, BigDecimal gross) {
    }

    /**
     * Generates the invoice XML.
     *
     * @param company issuing company
     * @param invoice invoice data including the customer
     * @return the XML document as UTF-8 bytes
     */
    public static byte[] xml(Company company, InvoiceData invoice) {
        try (LibraryClassLoaderScope scope = LibraryClassLoaderScope.enter()) {
            ZUGFeRD2PullProvider provider = new ZUGFeRD2PullProvider();
            provider.setProfile(Profiles.getByName(PROFILE));
            provider.generateXML(build(company, invoice));
            return provider.getXML();
        }
    }

    /**
     * Calculates the totals exactly as they will appear in the XML.
     *
     * @param company issuing company
     * @param invoice invoice data including the customer
     * @return net, tax and gross amount; all zero for an invoice without lines
     */
    public static Totals totals(Company company, InvoiceData invoice) {
        if (invoice.getItems().isEmpty()) {
            return new Totals(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        TransactionCalculator calculator = new TransactionCalculator(build(company, invoice));
        BigDecimal net = calculator.getTaxBasis();
        BigDecimal gross = calculator.getGrandTotal();
        return new Totals(net, gross.subtract(net), gross);
    }

    private static Invoice build(Company company, InvoiceData data) {
        Customer customer = data.getCustomer() == null ? new Customer() : data.getCustomer();
        Tax tax = Tax.of(company, customer);
        // Category O (not subject to VAT) must not carry any VAT ID, neither the seller's nor the buyer's.
        boolean withVatIds = !NOT_SUBJECT_TO_VAT.equals(tax.category());

        LocalDate date = data.getDate() == null ? LocalDate.now() : data.getDate();
        Invoice invoice = new Invoice()
                .setNumber(data.getNumber())
                .setIssueDate(toDate(date))
                .setDueDate(toDate(date.plusDays(company.getPaymentDays())))
                .setSender(seller(company, withVatIds))
                .setRecipient(buyer(customer, withVatIds))
                .setCurrency(customer.getCurrency());
        setServicePeriod(invoice, data, date);
        if (filled(customer.getBuyerReference())) {
            invoice.setReferenceNumber(customer.getBuyerReference().trim());
        }
        String paymentText = Placeholders.fill(paymentText(company, customer), company, data);
        if (filled(paymentText)) {
            invoice.setPaymentTermDescription(paymentText.trim());
        }
        for (LineItem item : data.getItems()) {
            invoice.addItem(line(item, Placeholders.fill(item.getText(), company, data), tax));
        }
        return invoice;
    }

    /**
     * English invoices use the English payment text if the company has one, otherwise the German text.
     * Without any text of its own the company gets the standard wording in the invoice language, because
     * the library would otherwise insert a text of its own.
     */
    private static String paymentText(Company company, Customer customer) {
        boolean english = customer.getInvoiceLanguage() == Language.ENGLISH;
        if (english && filled(company.getPaymentTextEnglish())) {
            return company.getPaymentTextEnglish();
        }
        if (filled(company.getPaymentText())) {
            return company.getPaymentText();
        }
        return InvoiceTexts.of(customer.getInvoiceLanguage()).get("payment.default");
    }

    private static TradeParty seller(Company company, boolean withVatId) {
        TradeParty seller = new TradeParty(company.getName(), company.getStreet(), company.getZip(),
                company.getCity(), countryCode(company.getCountry()));
        boolean hasVatId = withVatId && filled(company.getVatId());
        if (hasVatId) {
            seller.addVATID(compact(company.getVatId()));
        }
        if (filled(company.getTaxNumber())) {
            seller.addTaxID(company.getTaxNumber().trim());
        }
        addSellerIdentifier(seller, company, hasVatId);
        if (filled(company.getIban())) {
            BankDetails bank = new BankDetails(compact(company.getIban()));
            if (filled(company.getBic())) {
                bank.setBIC(compact(company.getBic()));
            }
            bank.setAccountName(company.getName());
            seller.addBankDetails(bank);
        }
        if (filled(company.getEmail())) {
            seller.setEmail(company.getEmail().trim());
        }
        if (filled(company.getContact()) || filled(company.getPhone())) {
            String contactName = filled(company.getContact()) ? company.getContact() : company.getName();
            seller.setContact(new Contact(contactName, company.getPhone(), company.getEmail()));
        }
        return seller;
    }

    /**
     * The standard requires a seller identifier, a legal registration number or a VAT ID (rule BR-CO-26).
     * The register number is always included if there is one. Without a register number and without a
     * VAT ID the tax number serves as the seller identifier.
     */
    private static void addSellerIdentifier(TradeParty seller, Company company, boolean hasVatId) {
        if (filled(company.getRegisterNumber())) {
            seller.setLegalOrganisation(new LegalOrganisation(company.getRegisterNumber().trim()));
        } else if (!hasVatId && filled(company.getTaxNumber())) {
            seller.setID(company.getTaxNumber().trim());
        }
    }

    private static TradeParty buyer(Customer customer, boolean withVatId) {
        TradeParty buyer = new TradeParty(customer.getName(), customer.getStreet(), customer.getZip(),
                customer.getCity(), countryCode(customer.getCountry()));
        if (filled(customer.getAddressExtra())) {
            buyer.setAdditionalAddress(customer.getAddressExtra());
        }
        if (withVatId && filled(customer.getVatId())) {
            buyer.addVATID(compact(customer.getVatId()));
        }
        if (filled(customer.getEmail())) {
            buyer.setEmail(customer.getEmail().trim());
        }
        return buyer;
    }

    private static void setServicePeriod(Invoice invoice, InvoiceData data, LocalDate invoiceDate) {
        if (data.getServiceFrom() == null || data.getServiceTo() == null) {
            invoice.setDeliveryDate(toDate(invoiceDate));
            return;
        }
        invoice.setDeliveryDate(toDate(data.getServiceTo()));
        invoice.setDetailedDeliveryPeriod(toDate(data.getServiceFrom()), toDate(data.getServiceTo()));
    }

    /** The first text line becomes the item name, the remaining lines its description. */
    private static Item line(LineItem item, String resolvedText, Tax tax) {
        String text = resolvedText.strip();
        int lineBreak = text.indexOf('\n');
        String name = lineBreak < 0 ? text : text.substring(0, lineBreak).strip();
        String description = lineBreak < 0 ? "" : text.substring(lineBreak + 1).strip();

        Product product = new Product(name, description, Units.code(item.getUnit()), tax.rate());
        if (!tax.standard()) {
            product.setTaxCategoryCode(tax.category());
            product.setTaxExemptionReason(tax.reason());
        }
        BigDecimal unitPrice = item.getUnitPrice() == null ? BigDecimal.ZERO : item.getUnitPrice();
        BigDecimal quantity = item.getQuantity() == null ? BigDecimal.ZERO : item.getQuantity();
        return new Item(product, unitPrice, quantity);
    }

    private static Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static boolean filled(String text) {
        return text != null && !text.isBlank();
    }

    private static String compact(String text) {
        return text.replaceAll("\\s+", "");
    }

    private static String countryCode(String country) {
        return filled(country) ? country.trim().toUpperCase() : DEFAULT_COUNTRY;
    }
}
