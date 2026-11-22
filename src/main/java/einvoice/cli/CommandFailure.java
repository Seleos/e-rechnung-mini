package einvoice.cli;

import java.io.Serial;
import java.util.List;

/** A command could not be carried out. The message is shown to the user and decides the exit code. */
final class CommandFailure extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int exitCode;
    private final List<String> details;

    private CommandFailure(int exitCode, String message, List<String> details) {
        super(message);
        this.exitCode = exitCode;
        this.details = List.copyOf(details);
    }

    /**
     * The arguments are wrong: an unknown command or option, a missing or malformed value.
     *
     * @param message what is wrong, in the selected language
     * @return the failure with exit code {@link CommandLine#USAGE}
     */
    static CommandFailure usage(String message) {
        return new CommandFailure(CommandLine.USAGE, message, List.of());
    }

    /**
     * The arguments are fine, but the data does not allow the command, for example an incomplete invoice.
     *
     * @param message what went wrong, in the selected language
     * @param details individual problems, may be empty
     * @return the failure with exit code {@link CommandLine#FAILURE}
     */
    static CommandFailure failure(String message, List<String> details) {
        return new CommandFailure(CommandLine.FAILURE, message, details);
    }

    int exitCode() {
        return exitCode;
    }

    List<String> details() {
        return details;
    }
}
