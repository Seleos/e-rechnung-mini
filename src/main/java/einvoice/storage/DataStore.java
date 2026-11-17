package einvoice.storage;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import einvoice.model.Company;
import einvoice.model.Customer;
import einvoice.model.InvoiceData;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * File based storage: one readable JSON file per company, customer and invoice record.
 * <pre>
 * data folder/
 *   companies/&lt;id&gt;/company.json, logo, template.xsl (optional)
 *   companies/&lt;id&gt;/customers/&lt;id&gt;.json   (customer with its services and rates)
 *   companies/&lt;id&gt;/records/&lt;number&gt;.json
 *   companies/&lt;id&gt;/invoices/&lt;year&gt;/   (default output folder)
 * </pre>
 */
public final class DataStore {
    private static final String COMPANY_FILE = "company.json";
    private static final String TEMPLATE_FILE = "template.xsl";

    private final Path root;
    private final List<Path> unreadableFiles = new ArrayList<>();
    private final ObjectMapper json = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * Creates a store on the given data folder. The folder is created when something is saved.
     *
     * @param root data folder
     */
    public DataStore(Path root) {
        this.root = root;
    }

    /**
     * Returns the data folder.
     *
     * @return root folder of this store
     */
    public Path root() {
        return root;
    }

    // ---- Companies ----

    /**
     * Loads all companies.
     *
     * @return the companies sorted by name
     */
    public List<Company> companies() {
        List<Company> companies = new ArrayList<>();
        for (Path dir : subDirectories(root.resolve("companies"))) {
            Path file = dir.resolve(COMPANY_FILE);
            if (Files.isRegularFile(file)) {
                readInto(companies, file, Company.class);
            }
        }
        companies.sort(Comparator.comparing(company -> company.getName().toLowerCase()));
        return companies;
    }

    /**
     * Saves a company. A new company gets an id derived from its name.
     *
     * @param company company to save
     */
    public void save(Company company) {
        if (company.getId() == null || company.getId().isBlank()) {
            company.setId(uniqueId(root.resolve("companies"), company.getName(), ""));
        }
        write(companyDir(company).resolve(COMPANY_FILE), company);
    }

    /**
     * Returns the folder that holds everything belonging to a company.
     *
     * @param company a saved company
     * @return its folder inside the data folder
     */
    public Path companyDir(Company company) {
        return root.resolve("companies").resolve(company.getId());
    }

    /**
     * Returns the logo of a company.
     *
     * @param company a saved company
     * @return the logo file, or null if none is set
     */
    public Path logoFile(Company company) {
        if (company.getLogo() == null || company.getLogo().isBlank()) {
            return null;
        }
        Path file = companyDir(company).resolve(company.getLogo());
        return Files.isRegularFile(file) ? file : null;
    }

    /**
     * Copies the chosen image into the company folder and remembers it as the logo.
     *
     * @param company a saved company
     * @param source  image file chosen by the user
     * @throws IOException if the file cannot be copied
     */
    public void setLogo(Company company, Path source) throws IOException {
        String fileName = source.toFile().getName();
        String extension = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.')).toLowerCase() : "";
        Files.createDirectories(companyDir(company));
        removeLogo(company);
        company.setLogo("logo" + extension);
        Files.copy(source, companyDir(company).resolve(company.getLogo()), StandardCopyOption.REPLACE_EXISTING);
        save(company);
    }

    /**
     * Deletes the logo file. The company itself is not saved by this call.
     *
     * @param company a saved company
     * @throws IOException if the file cannot be deleted
     */
    public void removeLogo(Company company) throws IOException {
        Path current = logoFile(company);
        if (current != null) {
            Files.delete(current);
        }
        company.setLogo("");
    }

    /**
     * Returns the company specific stylesheet.
     *
     * @param company a saved company
     * @return the stylesheet file, or null if the built-in layout is used
     */
    public Path templateFile(Company company) {
        Path file = companyDir(company).resolve(TEMPLATE_FILE);
        return Files.isRegularFile(file) ? file : null;
    }

    /**
     * Puts a copy of the built-in stylesheet into the company folder so it can be adapted.
     * An existing stylesheet is left untouched.
     *
     * @param company a saved company
     * @return the stylesheet file in the company folder
     * @throws IOException if the file cannot be written
     */
    public Path exportTemplate(Company company) throws IOException {
        Path target = companyDir(company).resolve(TEMPLATE_FILE);
        if (!Files.exists(target)) {
            Files.createDirectories(companyDir(company));
            try (InputStream in = DataStore.class.getResourceAsStream("/einvoice/template.xsl")) {
                Files.copy(in, target);
            }
        }
        return target;
    }

    // ---- Customers ----

    /**
     * Loads the customers of a company.
     *
     * @param company a saved company
     * @return its customers sorted by name
     */
    public List<Customer> customers(Company company) {
        List<Customer> customers = readAll(companyDir(company).resolve("customers"), Customer.class);
        customers.sort(Comparator.comparing(customer -> customer.getName().toLowerCase()));
        return customers;
    }

    /**
     * Saves a customer. A new customer gets an id derived from its name.
     *
     * @param company  company the customer belongs to
     * @param customer customer to save
     */
    public void save(Company company, Customer customer) {
        Path dir = companyDir(company).resolve("customers");
        if (customer.getId() == null || customer.getId().isBlank()) {
            customer.setId(uniqueId(dir, customer.getName(), ".json"));
        }
        write(dir.resolve(customer.getId() + ".json"), customer);
    }

    /**
     * Deletes a customer. Invoice records keep their own copy of the customer.
     *
     * @param company  company the customer belongs to
     * @param customer customer to delete
     * @throws IOException if the file cannot be deleted
     */
    public void delete(Company company, Customer customer) throws IOException {
        Files.deleteIfExists(companyDir(company).resolve("customers").resolve(customer.getId() + ".json"));
    }

    // ---- Invoice records ----

    /**
     * Loads the records of all invoices created for a company.
     *
     * @param company a saved company
     * @return the records in descending order of their invoice number, compared as text
     */
    public List<InvoiceData> records(Company company) {
        List<InvoiceData> records = readAll(companyDir(company).resolve("records"), InvoiceData.class);
        records.sort(Comparator.comparing((InvoiceData record) -> record.getNumber()).reversed());
        return records;
    }

    /**
     * Stores the record of a created invoice. An existing record is never replaced.
     *
     * @param company issuing company
     * @param invoice invoice data including totals and file name
     * @throws UncheckedIOException if a record with this number exists or the file cannot be written
     */
    public void saveRecord(Company company, InvoiceData invoice) {
        if (hasRecord(company, invoice.getNumber())) {
            String existing = recordFile(company, invoice.getNumber()).toString();
            throw new UncheckedIOException(new FileAlreadyExistsException(existing));
        }
        write(recordFile(company, invoice.getNumber()), invoice);
    }

    /**
     * Tells whether an invoice with this number was already created for the company.
     *
     * @param company issuing company
     * @param number  invoice number
     * @return true if a record with this number exists
     */
    public boolean hasRecord(Company company, String number) {
        return Files.exists(recordFile(company, number));
    }

    private Path recordFile(Company company, String number) {
        return companyDir(company).resolve("records").resolve(number + ".json");
    }

    /**
     * Returns the files that could not be read since the last call, for example after a sync conflict.
     * They were skipped so the rest of the data stays usable.
     *
     * @return the unreadable files; the internal list is emptied by this call
     */
    public List<Path> takeUnreadableFiles() {
        List<Path> files = List.copyOf(unreadableFiles);
        unreadableFiles.clear();
        return files;
    }

    /**
     * Returns the folder for finished invoices. The folder is not created by this call.
     *
     * @param company issuing company
     * @param year    invoice year, replaces {year} in the configured path
     * @return the configured output folder, or the default inside the company folder
     */
    public Path outputDir(Company company, int year) {
        if (company.getOutputDir() == null || company.getOutputDir().isBlank()) {
            return companyDir(company).resolve("invoices").resolve(String.valueOf(year));
        }
        String path = company.getOutputDir().trim().replace("{year}", String.valueOf(year));
        if (path.startsWith("~")) {
            path = System.getProperty("user.home") + path.substring(1);
        }
        return Path.of(path);
    }

    /**
     * Proposes the next free invoice number. Looks at the files in the output folder and at the stored
     * records, so numbers written by hand or by another computer are respected.
     *
     * @param company issuing company with its number prefix
     * @param date    invoice date; its year selects the number range
     * @return the highest number found plus one, formatted with the configured digits
     */
    public String nextNumber(Company company, LocalDate date) {
        String prefix = company.getNumberPrefix().replace("{year}", String.valueOf(date.getYear()));
        // At most nine digits, so a file name with a longer run of digits cannot overflow the counter.
        Pattern pattern = Pattern.compile(Pattern.quote(prefix) + "(\\d{1,9})(\\D.*)?");
        int highest = 0;
        List<String> names = new ArrayList<>(fileNames(outputDir(company, date.getYear())));
        names.addAll(fileNames(companyDir(company).resolve("records")));
        for (String name : names) {
            Matcher matcher = pattern.matcher(name);
            if (matcher.matches()) {
                highest = Math.max(highest, Integer.parseInt(matcher.group(1)));
            }
        }
        return prefix + String.format("%0" + Math.max(1, company.getNumberDigits()) + "d", highest + 1);
    }

    // ---- Helpers ----

    private <T> List<T> readAll(Path dir, Class<T> type) {
        List<T> result = new ArrayList<>();
        for (String name : fileNames(dir)) {
            if (name.endsWith(".json")) {
                readInto(result, dir.resolve(name), type);
            }
        }
        return result;
    }

    /** Reads one file; a damaged file is remembered and skipped instead of making all data unusable. */
    private <T> void readInto(List<T> target, Path file, Class<T> type) {
        try {
            target.add(json.readValue(file.toFile(), type));
        } catch (IOException e) {
            unreadableFiles.add(file);
        }
    }

    /** Writes to a temporary file first, so a crash or a sync in the middle never leaves half a file. */
    private void write(Path file, Object value) {
        try {
            createParentDirectories(file);
            Path temporary = file.resolveSibling(file.toFile().getName() + ".tmp");
            json.writeValue(temporary.toFile(), value);
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void createParentDirectories(Path file) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private static List<Path> subDirectories(Path dir) {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> entries = Files.list(dir)) {
            return entries.filter(Files::isDirectory).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<String> fileNames(Path dir) {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> entries = Files.list(dir)) {
            return entries.filter(Files::isRegularFile).map(path -> path.toFile().getName()).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Readable, file-system safe id derived from a name, made unique inside the folder. */
    private static String uniqueId(Path dir, String name, String extension) {
        String lowerCase = name == null ? "" : name.toLowerCase()
                .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss");
        String base = Normalizer.normalize(lowerCase, Normalizer.Form.NFD)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (base.isEmpty()) {
            base = "entry";
        }
        String id = base;
        int counter = 2;
        while (Files.exists(dir.resolve(id + extension))) {
            id = base + "-" + counter++;
        }
        return id;
    }
}
