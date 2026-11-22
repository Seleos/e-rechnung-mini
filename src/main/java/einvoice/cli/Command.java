package einvoice.cli;

import java.util.Set;

/** One command of the command line, such as {@code invoice list} or {@code invoice create}. */
interface Command {
    /**
     * Names of the options and flags the command accepts in addition to the common ones.
     *
     * @return option names without the leading dashes
     */
    Set<String> options();

    /**
     * Carries out the command.
     *
     * @param arguments parsed arguments, already checked against {@link #options()}
     * @param context   data folder, today's date and the output
     * @throws CommandFailure if the arguments are wrong or the command cannot be carried out
     */
    void run(Arguments arguments, CommandContext context) throws CommandFailure;
}
