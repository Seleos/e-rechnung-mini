package einvoice.core;

import einvoice.TestData;
import einvoice.model.Company;
import einvoice.model.Customer;
import einvoice.model.TaxCase;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Second opinion on the invoice XML. The app generates and validates with the same library, so a mistake
 * in that library could pass unnoticed. This test applies the official EN 16931 business rules as published
 * by the European standards committee (CEN/TC 434), independently of the library used by the app.
 */
class IndependentRulesTest {
    @Test
    void domesticStandardRatePassesTheOfficialRules() throws Exception {
        assertPasses(TestData.company(), TestData.customer(TaxCase.DOMESTIC_19, "DE", "DE111111111"));
    }

    @Test
    void domesticReducedRatePassesTheOfficialRules() throws Exception {
        assertPasses(TestData.company(), TestData.customer(TaxCase.DOMESTIC_7, "DE", ""));
    }

    @Test
    void reverseChargePassesTheOfficialRules() throws Exception {
        assertPasses(TestData.company(), TestData.customer(TaxCase.EU_REVERSE_CHARGE, "FR", "FR12345678901"));
    }

    @Test
    void thirdCountryPassesTheOfficialRules() throws Exception {
        assertPasses(TestData.company(), TestData.customer(TaxCase.THIRD_COUNTRY, "US", ""));
    }

    @Test
    void foreignCurrencyPassesTheOfficialRules() throws Exception {
        Customer customer = TestData.customer(TaxCase.THIRD_COUNTRY, "US", "");
        customer.setCurrency("USD");
        assertPasses(TestData.company(), customer);
    }

    @Test
    void smallBusinessPassesTheOfficialRules() throws Exception {
        Company smallBusiness = TestData.company();
        smallBusiness.setSmallBusiness(true);
        smallBusiness.setVatId("");
        assertPasses(smallBusiness, TestData.customer(TaxCase.DOMESTIC_19, "DE", ""));
    }

    @Test
    void soleProprietorWithoutRegisterNumberPassesTheOfficialRules() throws Exception {
        Company soleProprietor = TestData.company();
        soleProprietor.setSmallBusiness(true);
        soleProprietor.setVatId("");
        soleProprietor.setRegisterNumber("");
        assertPasses(soleProprietor, TestData.customer(TaxCase.DOMESTIC_19, "DE", ""));
    }

    @Test
    void ruleSetCatchesAnInvoiceWithoutNumber() throws Exception {
        String valid = new String(XmlBuilder.xml(TestData.company(), TestData.invoice(TestData.domesticCustomer())),
                StandardCharsets.UTF_8);
        String withoutNumber = valid.replace("<ram:ID>RE-2026-00043</ram:ID>", "");

        List<String> violations = OfficialRules.violations(withoutNumber.getBytes(StandardCharsets.UTF_8));

        assertTrue(violations.stream().anyMatch(violation -> violation.contains("BR-02")), violations.toString());
    }

    private static void assertPasses(Company company, Customer customer) throws Exception {
        assertEquals(List.of(), OfficialRules.violations(XmlBuilder.xml(company, TestData.invoice(customer))));
    }
}
