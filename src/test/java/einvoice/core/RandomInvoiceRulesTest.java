package einvoice.core;

import einvoice.TestData;
import einvoice.model.Company;
import einvoice.model.Customer;
import einvoice.model.InvoiceData;
import einvoice.model.LineItem;
import einvoice.model.TaxCase;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Generates many invoices with random quantities, prices and tax cases and checks each one against the
 * official EN 16931 rules, which include the arithmetic of line totals, VAT and the amount due. This finds
 * rounding errors nobody thought of. The seed is fixed, so a failure can be reproduced.
 */
class RandomInvoiceRulesTest {
    private static final long SEED = 20261117L;
    private static final int INVOICES = 300;
    private static final int MAX_LINES = 12;
    private static final String[] UNITS = {"h", "Stk", "Tag", "Monat", "pauschal", "km", "min"};

    @Test
    void randomInvoicesPassTheOfficialRulesAndAddUp() throws Exception {
        Random random = new Random(SEED);
        for (int number = 1; number <= INVOICES; number++) {
            Company company = TestData.company();
            company.setSmallBusiness(random.nextInt(10) == 0);
            InvoiceData invoice = randomInvoice(random);
            String description = "invoice " + number + " of seed " + SEED + ": " + describe(company, invoice);

            assertEquals(List.of(), OfficialRules.violations(XmlBuilder.xml(company, invoice)), description);
            assertTotalsAddUp(company, invoice, description);
        }
    }

    private static InvoiceData randomInvoice(Random random) {
        TaxCase taxCase = TaxCase.values()[random.nextInt(TaxCase.values().length)];
        Customer customer = switch (taxCase) {
            case EU_REVERSE_CHARGE -> TestData.customer(taxCase, "FR", "FR12345678901");
            case THIRD_COUNTRY -> TestData.customer(taxCase, "US", "");
            default -> TestData.customer(taxCase, "DE", "DE111111111");
        };
        InvoiceData invoice = TestData.invoice(customer);
        invoice.getItems().clear();
        int lines = 1 + random.nextInt(MAX_LINES);
        for (int line = 0; line < lines; line++) {
            // Quantities with two decimals; prices with two and sometimes four decimals, the awkward case.
            BigDecimal quantity = BigDecimal.valueOf(1 + random.nextInt(50_000), 2);
            int priceScale = random.nextInt(4) == 0 ? 4 : 2;
            int highestPrice = priceScale == 4 ? 9_999_999 : 499_999;
            BigDecimal price = BigDecimal.valueOf(1 + random.nextInt(highestPrice), priceScale);
            String unit = UNITS[random.nextInt(UNITS.length)];
            invoice.getItems().add(new LineItem(quantity, unit, "Leistung " + line, price));
        }
        return invoice;
    }

    /** The net amount is the sum of the rounded line totals, and the amount due is net plus VAT. */
    private static void assertTotalsAddUp(Company company, InvoiceData invoice, String description) {
        XmlBuilder.Totals totals = XmlBuilder.totals(company, invoice);
        BigDecimal expectedNet = BigDecimal.ZERO;
        for (LineItem item : invoice.getItems()) {
            BigDecimal lineTotal = item.getQuantity().multiply(item.getUnitPrice());
            expectedNet = expectedNet.add(lineTotal.setScale(2, RoundingMode.HALF_UP));
        }
        Tax tax = Tax.of(company, invoice.getCustomer());
        BigDecimal expectedTax = expectedNet.multiply(tax.rate())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        assertEquals(0, expectedNet.compareTo(totals.net()), "net amount of " + description);
        assertEquals(0, expectedTax.compareTo(totals.tax()), "VAT of " + description);
        assertEquals(0, expectedNet.add(expectedTax).compareTo(totals.gross()), "amount due of " + description);
    }

    private static String describe(Company company, InvoiceData invoice) {
        StringBuilder text = new StringBuilder(invoice.getCustomer().getTaxCase().name());
        if (company.isSmallBusiness()) {
            text.append(", small business");
        }
        for (LineItem item : invoice.getItems()) {
            text.append(", ").append(item.getQuantity()).append(" x ").append(item.getUnitPrice());
        }
        return text.toString();
    }
}
