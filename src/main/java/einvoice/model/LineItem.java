package einvoice.model;

import java.math.BigDecimal;

/** One invoice line. The first text line is the item name, further lines are its description. */
public class LineItem {
    private BigDecimal quantity = BigDecimal.ONE;
    private String unit = "h";
    private String text = "";
    private BigDecimal unitPrice = BigDecimal.ZERO;

    /** Creates an empty line; needed for reading stored invoices. */
    public LineItem() {
    }

    /**
     * Creates a line.
     *
     * @param quantity  billed quantity
     * @param unit      display name of the unit, for example "h"
     * @param text      item name, optionally followed by description lines
     * @param unitPrice net price per unit
     */
    public LineItem(BigDecimal quantity, String unit, String text, BigDecimal unitPrice) {
        this.quantity = quantity;
        this.unit = unit;
        this.text = text;
        this.unitPrice = unitPrice;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
}
