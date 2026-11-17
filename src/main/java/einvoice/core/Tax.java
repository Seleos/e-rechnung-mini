package einvoice.core;

import einvoice.i18n.InvoiceTexts;
import einvoice.i18n.Messages;
import einvoice.model.Company;
import einvoice.model.Customer;
import einvoice.model.TaxCase;

import java.math.BigDecimal;

/**
 * Tax treatment of an invoice, derived from company and customer.
 *
 * @param category VAT category of the standard: S (standard), E (exempt), AE (reverse charge), O (not subject to VAT)
 * @param rate     VAT rate in percent
 * @param reason   mandatory note for 0 % cases in the invoice language, otherwise empty
 * @param label    short name shown in the UI
 */
public record Tax(String category, BigDecimal rate, String reason, String label) {

    /**
     * Determines the tax treatment of an invoice.
     *
     * @param company  issuing company; a small business never charges VAT
     * @param customer recipient whose tax case applies otherwise, may be null
     * @return the tax rule for all lines of the invoice
     */
    public static Tax of(Company company, Customer customer) {
        if (company.isSmallBusiness()) {
            return new Tax("E", BigDecimal.ZERO,
                    invoiceTexts(customer).get("tax.reason.smallBusiness"),
                    Messages.get("tax.smallBusiness"));
        }
        TaxCase taxCase = customer == null || customer.getTaxCase() == null
                ? TaxCase.DOMESTIC_19
                : customer.getTaxCase();
        return switch (taxCase) {
            case DOMESTIC_19 -> new Tax("S", new BigDecimal("19"), "", Messages.get("tax.domestic19"));
            case DOMESTIC_7 -> new Tax("S", new BigDecimal("7"), "", Messages.get("tax.domestic7"));
            case EU_REVERSE_CHARGE -> new Tax("AE", BigDecimal.ZERO,
                    invoiceTexts(customer).get("tax.reason.reverseCharge"),
                    Messages.get("tax.reverseCharge"));
            case THIRD_COUNTRY -> new Tax("O", BigDecimal.ZERO,
                    invoiceTexts(customer).get("tax.reason.thirdCountry"), Messages.get("tax.thirdCountry"));
        };
    }

    /** The mandatory note is printed on the invoice, so it follows the invoice language of the customer. */
    private static InvoiceTexts invoiceTexts(Customer customer) {
        return InvoiceTexts.of(customer == null ? null : customer.getInvoiceLanguage());
    }

    /**
     * Tells whether VAT is charged at a regular rate.
     *
     * @return true for category S, false for all 0 % cases
     */
    public boolean standard() {
        return "S".equals(category);
    }

    /**
     * Returns the label of the VAT row in the totals.
     *
     * @return the label shown next to the VAT amount
     */
    public String rowLabel() {
        return standard()
                ? Messages.get("tax.row.standard", rate.toPlainString())
                : Messages.get("tax.row.zero");
    }
}
