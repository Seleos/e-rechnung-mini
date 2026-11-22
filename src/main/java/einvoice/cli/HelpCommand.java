package einvoice.cli;

import einvoice.AppInfo;
import einvoice.core.Units;
import einvoice.i18n.Messages;

import java.util.Set;

/** {@code help}: explains the commands and options. */
final class HelpCommand implements Command {
    @Override
    public Set<String> options() {
        return Set.of();
    }

    @Override
    public void run(Arguments arguments, CommandContext context) {
        context.out().println(Messages.get("cli.title", AppInfo.name(), AppInfo.version()));
        context.out().println();
        // Printed without arguments, so the placeholders in the text such as {period} stay as they are.
        context.out().println(Messages.get("cli.usage"));
        context.out().println(Messages.get("cli.units", String.join(", ", Units.all())));
    }
}
