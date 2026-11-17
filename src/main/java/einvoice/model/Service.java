package einvoice.model;

import java.math.BigDecimal;

/** Service offered to a customer at an agreed rate; can be inserted into an invoice as a line item. */
public class Service {
    private String text = "";
    private String unit = "h";
    private BigDecimal unitPrice = BigDecimal.ZERO;
    /** Whether a new invoice for the customer starts with this service as a line item. */
    private boolean includedByDefault;

    /** Creates an empty service; needed for reading stored services. */
    public Service() {
    }

    /**
     * Creates a service.
     *
     * @param text      item name
     * @param unit      display name of the unit, for example "h"
     * @param unitPrice net price per unit
     */
    public Service(String text, String unit, BigDecimal unitPrice) {
        this.text = text;
        this.unit = unit;
        this.unitPrice = unitPrice;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    /**
     * Tells whether a new invoice for the customer starts with this service as a line item.
     *
     * @return true if the service is pre-filled
     */
    public boolean isIncludedByDefault() {
        return includedByDefault;
    }

    public void setIncludedByDefault(boolean includedByDefault) {
        this.includedByDefault = includedByDefault;
    }
}
