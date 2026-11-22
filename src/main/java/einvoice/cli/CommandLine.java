package einvoice.cli;

import einvoice.i18n.Messages;
import einvoice.storage.AppSettings;
import einvoice.storage.DataStore;

import java.io.PrintStream;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Runs one command given on the command line and reports the result through the exit code:
 * {@link #SUCCESS}, {@link #FAILURE} if the data does not allow the command, or {@link #USAGE} if the
 * arguments are wrong. Works on the same data folder and in the same language as the user interface.
 */
public final class CommandLine {
    /** Exit code: the command was carried out. */
    public static final int SUCCESS = 0;

    /** Exit code: the arguments are fine, but the command could not be carried out. */
    public static final int FAILURE = 1;

    /** Exit code: unknown command or option, or a missing or malformed value. */
    public static final int USAGE = 2;

    private static final String HELP = "help";
    private static final String VERSION = "version";
    private static final String DATA_DIR = "data-dir";
    private static final Set<String> FLAGS = Set.of(HELP, InvoiceCreateCommand.DRY_RUN);
    private static final Map<String, Command> COMMANDS = commands();
    /** First words of the commands that consist of an entity and an action, such as {@code invoice}. */
    private static final Set<String> ENTITIES = COMMANDS.keySet().stream()
            .filter(name -> name.contains(" "))
            .map(CommandLine::entity)
            .collect(Collectors.toUnmodifiableSet());

    private final Path dataDir;
    private final Clock clock;
    private final PrintStream out;
    private final PrintStream err;

    /**
     * Creates a command line on a data folder.
     *
     * @param dataDir data folder used unless {@code --data-dir} names another one
     * @param clock   source of today's date
     * @param out     target of regular output
     * @param err     target of error messages
     */
    public CommandLine(Path dataDir, Clock clock, PrintStream out, PrintStream err) {
        this.dataDir = dataDir;
        this.clock = clock;
        this.out = out;
        this.err = err;
    }

    /**
     * Creates the command line with the data folder and language configured in the user interface.
     *
     * @return a command line that prints to the console
     */
    public static CommandLine forInstallation() {
        AppSettings settings = new AppSettings();
        Messages.setLanguage(settings.language());
        return new CommandLine(settings.dataDir(), Clock.systemDefaultZone(), System.out, System.err);
    }

    /**
     * Tells whether the arguments ask for the command line instead of the window.
     *
     * @param args arguments the program was started with
     * @return true if the first argument is a word, or {@code --help}, {@code -h} or {@code --version}; options
     *         the operating system passes to a starting app, such as {@code -psn_0_1234} on macOS, open the window
     */
    public static boolean isCommand(String[] args) {
        // A mistyped command must end with a message and an exit code, not with a window nobody asked for.
        return args.length > 0 && (!args[0].startsWith("-") || COMMANDS.containsKey(commandName(args[0])));
    }

    /**
     * Runs the command.
     *
     * @param args command followed by its options
     * @return the exit code
     */
    public int run(String... args) {
        try {
            String[] normalized = args.clone();
            if (normalized.length > 0) {
                normalized[0] = commandName(normalized[0]);
            }
            Arguments arguments = Arguments.parse(normalized, FLAGS);
            Command command;
            if (arguments.has(HELP)) {
                // Help is shown for any command line, even an incomplete one or one with rejected options.
                command = COMMANDS.get(HELP);
            } else {
                command = COMMANDS.get(arguments.command());
                if (command == null) {
                    throw unknownCommand(arguments.command());
                }
                Set<String> allowed = new HashSet<>(command.options());
                allowed.add(DATA_DIR);
                arguments.allowOnly(allowed);
            }
            DataStore store = new DataStore(arguments.value(DATA_DIR).map(Path::of).orElse(dataDir));
            try {
                command.run(arguments, new CommandContext(store, LocalDate.now(clock), out));
            } finally {
                reportUnreadableFiles(store);
            }
            return SUCCESS;
        } catch (CommandFailure failure) {
            err.println(failure.getMessage());
            failure.details().forEach(detail -> err.println(Messages.get("cli.detail", detail)));
            if (failure.exitCode() == USAGE) {
                err.println(Messages.get("cli.hint"));
            }
            return failure.exitCode();
        }
    }

    /** Damaged files are skipped so the rest stays usable, but the user has to learn about them. */
    private void reportUnreadableFiles(DataStore store) {
        for (Path file : store.takeUnreadableFiles()) {
            err.println(Messages.get("cli.warning.unreadable", file));
        }
    }

    /** Names the actions of a known entity, so a missing or mistyped action is easy to correct. */
    private static CommandFailure unknownCommand(String command) {
        String entity = entity(command);
        if (!ENTITIES.contains(entity)) {
            return CommandFailure.usage(Messages.get("cli.error.unknownCommand", command));
        }
        String actions = COMMANDS.keySet().stream()
                .filter(name -> name.startsWith(entity + " "))
                .map(name -> name.substring(entity.length() + 1))
                .sorted()
                .collect(Collectors.joining(", "));
        return CommandFailure.usage(Messages.get("cli.error.unknownAction", entity, actions));
    }

    private static String entity(String command) {
        int space = command.indexOf(' ');
        return space < 0 ? command : command.substring(0, space);
    }

    /** The usual spellings of help and version are accepted in place of the command. */
    private static String commandName(String argument) {
        return switch (argument) {
            case "--help", "-h" -> HELP;
            case "--version" -> VERSION;
            default -> argument;
        };
    }

    private static Map<String, Command> commands() {
        Map<String, Command> commands = new LinkedHashMap<>();
        commands.put("company list", new CompanyListCommand());
        commands.put("customer list", new CustomerListCommand());
        commands.put("invoice list", new InvoiceListCommand());
        commands.put("invoice create", new InvoiceCreateCommand());
        commands.put(HELP, new HelpCommand());
        commands.put(VERSION, new VersionCommand());
        return Map.copyOf(commands);
    }
}
