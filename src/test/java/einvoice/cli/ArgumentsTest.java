package einvoice.cli;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests reading options from the command line. */
class ArgumentsTest {
    private static final Set<String> FLAGS = Set.of("dry-run");

    @Test
    void readsValuesInBothSpellingsFlagsAndRepeatedOptionsInOrder() throws CommandFailure {
        Arguments arguments = Arguments.parse(new String[] {
            "invoice", "create", "--company", "musterfirma-gmbh", "--qty=2,5", "--dry-run", "--qty", "3"}, FLAGS);

        assertEquals("invoice create", arguments.command());
        assertEquals(Optional.of("musterfirma-gmbh"), arguments.value("company"));
        assertEquals(List.of("2,5", "3"), arguments.values("qty"));
        assertTrue(arguments.has("dry-run"));
        assertFalse(arguments.has("from"));
        assertEquals(Optional.empty(), arguments.value("from"));
        assertEquals(List.of(), arguments.values("from"));
    }

    @Test
    void valueMayStartWithDashesAndBeEmpty() throws CommandFailure {
        Arguments arguments = Arguments.parse(new String[] {"create", "--text", "--weird--", "--unit="}, FLAGS);

        assertEquals(Optional.of("--weird--"), arguments.value("text"));
        assertEquals(Optional.of(""), arguments.value("unit"));
    }

    @Test
    void rejectsMissingCommandStrayArgumentsAndMissingValues() {
        assertUsage(new String[] {});
        assertUsage(new String[] {" "});
        assertUsage(new String[] {"--company", "musterfirma-gmbh"});
        assertUsage(new String[] {"invoice", " ", "--year", "2026"});
        assertUsage(new String[] {"invoice", "list", "--year", "2026", "stray"});
        assertUsage(new String[] {"create", "--"});
        assertUsage(new String[] {"create", "--company"});
    }

    @Test
    void singleValueOptionMayNotBeRepeated() throws CommandFailure {
        Arguments arguments = Arguments.parse(new String[] {"list", "--year", "2025", "--year", "2026"}, FLAGS);

        CommandFailure failure = assertThrows(CommandFailure.class, () -> arguments.value("year"));
        assertEquals(CommandLine.USAGE, failure.exitCode());
    }

    @Test
    void allowOnlyRejectsUnknownOptionsAndFlags() throws CommandFailure {
        Arguments arguments = Arguments.parse(new String[] {"list", "--year", "2026", "--dry-run"}, FLAGS);

        assertDoesNotThrow(() -> arguments.allowOnly(Set.of("year", "dry-run")));
        assertThrows(CommandFailure.class, () -> arguments.allowOnly(Set.of("year")));
        assertThrows(CommandFailure.class, () -> arguments.allowOnly(Set.of("dry-run")));
    }

    private static void assertUsage(String[] args) {
        CommandFailure failure = assertThrows(CommandFailure.class, () -> Arguments.parse(args, FLAGS));
        assertEquals(CommandLine.USAGE, failure.exitCode());
        assertTrue(failure.details().isEmpty());
    }
}
