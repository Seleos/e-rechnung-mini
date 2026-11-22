package einvoice.cli;

import einvoice.i18n.Messages;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Parsed command line: the command, given as one or more words such as {@code invoice create}, followed by
 * options in the form {@code --name value}, {@code --name=value} or, for flags, {@code --name}. Options may be
 * repeated; their order is kept.
 */
final class Arguments {
    private static final String PREFIX = "--";

    private final String command;
    private final Map<String, List<String>> options;
    private final Set<String> flags;

    private Arguments(String command, Map<String, List<String>> options, Set<String> flags) {
        this.command = command;
        this.options = options;
        this.flags = flags;
    }

    /**
     * Parses the arguments.
     *
     * @param args      arguments as given on the command line; the words before the first option are the command
     * @param flagNames options that take no value
     * @return the parsed arguments
     * @throws CommandFailure if no command is given, an argument after the options is not an option or a value
     *                        is missing
     */
    static Arguments parse(String[] args, Set<String> flagNames) throws CommandFailure {
        List<String> words = new ArrayList<>();
        int index = 0;
        while (index < args.length && !args[index].startsWith(PREFIX)) {
            words.add(args[index++]);
        }
        if (words.isEmpty() || words.stream().anyMatch(String::isBlank)) {
            throw CommandFailure.usage(Messages.get("cli.error.noCommand"));
        }
        Map<String, List<String>> options = new LinkedHashMap<>();
        Set<String> flags = new LinkedHashSet<>();
        while (index < args.length) {
            String argument = args[index++];
            if (!argument.startsWith(PREFIX) || argument.length() == PREFIX.length()) {
                throw CommandFailure.usage(Messages.get("cli.error.notAnOption", argument));
            }
            String name = argument.substring(PREFIX.length());
            int equals = name.indexOf('=');
            if (equals >= 0) {
                options.computeIfAbsent(name.substring(0, equals), key -> new ArrayList<>())
                        .add(name.substring(equals + 1));
            } else if (flagNames.contains(name)) {
                flags.add(name);
            } else if (index < args.length) {
                options.computeIfAbsent(name, key -> new ArrayList<>()).add(args[index++]);
            } else {
                throw CommandFailure.usage(Messages.get("cli.error.missingValue", argument));
            }
        }
        return new Arguments(String.join(" ", words), options, flags);
    }

    /**
     * Returns the command.
     *
     * @return the words before the first option, separated by single spaces
     */
    String command() {
        return command;
    }

    /**
     * Rejects options the command does not know, so a typing error does not go unnoticed.
     *
     * @param allowed names of the options and flags the command accepts
     * @throws CommandFailure naming the first unknown option
     */
    void allowOnly(Set<String> allowed) throws CommandFailure {
        for (String name : names()) {
            if (!allowed.contains(name)) {
                throw CommandFailure.usage(Messages.get("cli.error.unknownOption", PREFIX + name));
            }
        }
    }

    /**
     * Returns the value of an option that may be given once.
     *
     * @param name option name without the leading dashes
     * @return the value, or empty if the option is not given
     * @throws CommandFailure if the option is given more than once
     */
    Optional<String> value(String name) throws CommandFailure {
        List<String> values = values(name);
        if (values.size() > 1) {
            throw CommandFailure.usage(Messages.get("cli.error.repeated", PREFIX + name));
        }
        return values.stream().findFirst();
    }

    /**
     * Returns all values of a repeatable option.
     *
     * @param name option name without the leading dashes
     * @return the values in the order given, empty if the option is not given
     */
    List<String> values(String name) {
        return List.copyOf(options.getOrDefault(name, List.of()));
    }

    /**
     * Tells whether a flag or an option is given.
     *
     * @param name option name without the leading dashes
     * @return true if it appears at least once
     */
    boolean has(String name) {
        return flags.contains(name) || options.containsKey(name);
    }

    private Set<String> names() {
        Set<String> names = new LinkedHashSet<>(options.keySet());
        names.addAll(flags);
        return names;
    }
}
