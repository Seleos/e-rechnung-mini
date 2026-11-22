package einvoice.cli;

import einvoice.AppInfo;

import java.util.Set;

/** {@code version}: prints the product name and version, for scripts and support requests. */
final class VersionCommand implements Command {
    @Override
    public Set<String> options() {
        return Set.of();
    }

    @Override
    public void run(Arguments arguments, CommandContext context) {
        context.out().println(AppInfo.name() + " " + AppInfo.version());
    }
}
