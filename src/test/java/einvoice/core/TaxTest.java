package einvoice.core;

import einvoice.TestData;
import einvoice.i18n.Language;
import einvoice.i18n.Messages;
import einvoice.model.Company;
import einvoice.model.TaxCase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests how the tax treatment follows from company and customer. */
class TaxTest {
    @BeforeAll
    static void useGerman() {
        Messages.setLanguage(Language.GERMAN);
    }

    @Test
    void domesticCustomersPayTheRegularRate() {
        Tax standard = Tax.of(TestData.company(), TestData.customer(TaxCase.DOMESTIC_19, "DE", ""));
        assertEquals("S", standard.category());
        assertEquals(new BigDecimal("19"), standard.rate());
        assertTrue(standard.standard());
        assertEquals("", standard.reason());
        assertEquals("zzgl. 19 % MwSt.", standard.rowLabel());

        Tax reduced = Tax.of(TestData.company(), TestData.customer(TaxCase.DOMESTIC_7, "DE", ""));
        assertEquals(new BigDecimal("7"), reduced.rate());
        assertTrue(reduced.standard());
    }

    @Test
    void reverseChargeAndThirdCountryCarryNoVatButANote() {
        Tax reverseCharge = Tax.of(TestData.company(), TestData.customer(TaxCase.EU_REVERSE_CHARGE, "FR", "FR1"));
        assertEquals("AE", reverseCharge.category());
        assertEquals(BigDecimal.ZERO, reverseCharge.rate());
        assertFalse(reverseCharge.standard());
        assertFalse(reverseCharge.reason().isBlank());
        assertEquals("Umsatzsteuer 0 %", reverseCharge.rowLabel());

        Tax thirdCountry = Tax.of(TestData.company(), TestData.customer(TaxCase.THIRD_COUNTRY, "US", ""));
        assertEquals("O", thirdCountry.category());
        assertFalse(thirdCountry.reason().isBlank());
    }

    @Test
    void smallBusinessNeverChargesVatWhateverTheCustomer() {
        Company smallBusiness = TestData.company();
        smallBusiness.setSmallBusiness(true);
        for (TaxCase taxCase : TaxCase.values()) {
            Tax tax = Tax.of(smallBusiness, TestData.customer(taxCase, "DE", ""));
            assertEquals("E", tax.category(), taxCase.name());
            assertTrue(tax.reason().contains("§ 19 UStG"), taxCase.name());
        }
    }

    @Test
    void missingCustomerIsTreatedAsDomestic() {
        assertEquals("S", Tax.of(TestData.company(), null).category());
    }
}
