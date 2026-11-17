package einvoice.model;

import einvoice.i18n.Messages;

/** Tax treatment of a customer; determines rate, category and mandatory note. */
public enum TaxCase {
    DOMESTIC_19("taxCase.domestic19"),
    DOMESTIC_7("taxCase.domestic7"),
    EU_REVERSE_CHARGE("taxCase.euReverseCharge"),
    THIRD_COUNTRY("taxCase.thirdCountry");

    private final String messageKey;

    TaxCase(String messageKey) {
        this.messageKey = messageKey;
    }

    /**
     * Returns the name shown to the user.
     *
     * @return the name in the selected language
     */
    public String label() {
        return Messages.get(messageKey);
    }

    @Override
    public String toString() {
        return label();
    }
}
