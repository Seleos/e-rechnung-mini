package einvoice.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Input data of an invoice. After creation it is stored as a record together with totals and file name. */
public class InvoiceData {
    private String number = "";
    private LocalDate date = LocalDate.now();
    private LocalDate serviceFrom;
    private LocalDate serviceTo;
    /** Copy of the customer at the time of invoicing. */
    private Customer customer;
    private List<LineItem> items = new ArrayList<>();

    // Only filled in the stored record
    private BigDecimal net;
    private BigDecimal tax;
    private BigDecimal gross;
    private String file;

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalDate getServiceFrom() {
        return serviceFrom;
    }

    public void setServiceFrom(LocalDate serviceFrom) {
        this.serviceFrom = serviceFrom;
    }

    public LocalDate getServiceTo() {
        return serviceTo;
    }

    public void setServiceTo(LocalDate serviceTo) {
        this.serviceTo = serviceTo;
    }

    /**
     * Returns the copy of the customer taken at the time of invoicing.
     *
     * @return the recipient, or null while none is selected
     */
    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public List<LineItem> getItems() {
        return items;
    }

    public void setItems(List<LineItem> items) {
        this.items = items;
    }

    public BigDecimal getNet() {
        return net;
    }

    public void setNet(BigDecimal net) {
        this.net = net;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public void setTax(BigDecimal tax) {
        this.tax = tax;
    }

    public BigDecimal getGross() {
        return gross;
    }

    public void setGross(BigDecimal gross) {
        this.gross = gross;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }
}
