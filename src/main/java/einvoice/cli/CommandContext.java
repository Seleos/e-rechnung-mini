package einvoice.cli;

import einvoice.i18n.Messages;
import einvoice.model.Company;
import einvoice.model.Customer;
import einvoice.storage.DataStore;

import java.io.PrintStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** What every command works with: the data folder, today's date and the output, plus shared look-ups. */
final class CommandContext {
    /** Option that selects the company; may be left out when the data folder holds only one. */
    static final String COMPANY = "company";

    private final DataStore store;
    private final LocalDate today;
    private final PrintStream out;

    /**
     * Creates the context.
     *
     * @param store data folder
     * @param today date used where none is given
     * @param out   target of regular output
     */
    CommandContext(DataStore store, LocalDate today, PrintStream out) {
        this.store = store;
        this.today = today;
        this.out = out;
    }

    DataStore store() {
        return store;
    }

    LocalDate today() {
        return today;
    }

    PrintStream out() {
        return out;
    }

    /**
     * Finds the company selected with {@code --company}, or the only one if the option is left out.
     *
     * @param arguments parsed arguments
     * @return the company
     * @throws CommandFailure if there is no company, the id is unknown, or several exist and none is chosen
     */
    Company company(Arguments arguments) throws CommandFailure {
        List<Company> companies = store.companies();
        if (companies.isEmpty()) {
            throw CommandFailure.failure(Messages.get("cli.error.noCompanies", store.root()), List.of());
        }
        Optional<String> id = arguments.value(COMPANY);
        if (id.isEmpty()) {
            if (companies.size() == 1) {
                return companies.get(0);
            }
            throw CommandFailure.usage(Messages.get("cli.error.companyRequired", ids(companies, Company::getId)));
        }
        return companies.stream()
                .filter(company -> company.getId().equals(id.get()))
                .findFirst()
                .orElseThrow(() -> CommandFailure.usage(
                        Messages.get("cli.error.unknownCompany", id.get(), ids(companies, Company::getId))));
    }

    /**
     * Finds a customer of a company by its id.
     *
     * @param company issuing company
     * @param id      customer id as shown by {@code customer list}
     * @return the stored customer
     * @throws CommandFailure if the company has no customer with this id
     */
    Customer customer(Company company, String id) throws CommandFailure {
        List<Customer> customers = store.customers(company);
        return customers.stream()
                .filter(customer -> customer.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> CommandFailure.usage(
                        Messages.get("cli.error.unknownCustomer", id, ids(customers, Customer::getId))));
    }

    /**
     * Reads a date option.
     *
     * @param arguments parsed arguments
     * @param name      option name without the leading dashes
     * @return the date, or empty if the option is not given
     * @throws CommandFailure if the value is not a date in the form YYYY-MM-DD
     */
    Optional<LocalDate> date(Arguments arguments, String name) throws CommandFailure {
        Optional<String> value = arguments.value(name);
        if (value.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(value.get().trim()));
        } catch (DateTimeParseException e) {
            throw CommandFailure.usage(Messages.get("cli.error.date", "--" + name, value.get()));
        }
    }

    /**
     * Formats a date the way the user interface shows it.
     *
     * @param date date, may be null
     * @return the date in the selected language, or an empty text for null
     */
    static String format(LocalDate date) {
        if (date == null) {
            return "";
        }
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(Messages.language().locale())
                .format(date);
    }

    private static <T> String ids(List<T> entries, Function<T, String> id) {
        return entries.stream().map(id).collect(Collectors.joining(", "));
    }
}
