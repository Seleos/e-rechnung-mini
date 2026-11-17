package einvoice.storage;

import einvoice.model.Company;
import einvoice.model.Customer;
import einvoice.model.Service;
import einvoice.model.TaxCase;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Creates a fictitious example company so a fresh installation is usable right away. */
public final class SampleData {
    private SampleData() {
    }

    /**
     * Creates the example company with a logo and three customers, each with its own services and rates.
     *
     * @param store data store to write to
     * @return the created company
     * @throws IOException if the sample logo cannot be written
     */
    public static Company create(DataStore store) throws IOException {
        Company company = new Company();
        company.setName("Musterfirma GmbH");
        company.setStreet("Musterstraße 1");
        company.setZip("10115");
        company.setCity("Berlin");
        company.setPhone("+49 30 0000000");
        company.setEmail("rechnung@musterfirma.example");
        company.setWeb("www.musterfirma.example");
        company.setContact("Erika Mustermann");
        company.setManagingDirector("Erika Mustermann");
        company.setRegisterCourt("Amtsgericht Musterstadt");
        company.setRegisterNumber("HRB 00000");
        company.setVatId("DE123456789");
        company.setTaxNumber("00/000/00000");
        company.setBankName("Musterbank");
        company.setIban("DE36 0000 0000 0000 0000 00");
        company.useStandardPaymentTexts();
        company.setBic("TESTDEFFXXX");
        store.save(company);

        Path logo = store.companyDir(company).resolve("logo.svg");
        try (InputStream in = SampleData.class.getResourceAsStream("/einvoice/sample-logo.svg")) {
            Files.copy(in, logo);
        }
        company.setLogo("logo.svg");
        store.save(company);

        store.save(company, customer("Beispielkunde GmbH", "Kundenallee 12", "14467", "Potsdam", "DE",
                "DE111111111", TaxCase.DOMESTIC_19,
                new Service("Wartung {month} {year}", "h", new BigDecimal("70.00")),
                new Service("Softwareentwicklung", "h", new BigDecimal("95.00"))));
        store.save(company, customer("Muster & Söhne KG", "Hafenstraße 3", "20457", "Hamburg", "DE",
                "", TaxCase.DOMESTIC_19,
                new Service("Wartung", "h", new BigDecimal("85.00")),
                new Service("Hosting, monatlich", "Stk", new BigDecimal("49.00"))));
        store.save(company, customer("Exemple SARL", "8 Rue de l'Exemple", "69001", "Lyon", "FR",
                "FR12345678901", TaxCase.EU_REVERSE_CHARGE,
                new Service("Softwareentwicklung", "h", new BigDecimal("110.00"))));
        return company;
    }

    private static Customer customer(String name, String street, String zip, String city, String country,
                                     String vatId, TaxCase taxCase, Service... services) {
        Customer customer = new Customer();
        customer.setName(name);
        customer.setStreet(street);
        customer.setZip(zip);
        customer.setCity(city);
        customer.setCountry(country);
        customer.setVatId(vatId);
        customer.setTaxCase(taxCase);
        customer.setServices(new ArrayList<>(List.of(services)));
        if (services.length > 0) {
            services[0].setIncludedByDefault(true);
        }
        return customer;
    }
}
