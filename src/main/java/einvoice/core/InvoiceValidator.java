package einvoice.core;

import org.mustangproject.validator.ESeverity;
import org.mustangproject.validator.ValidationResultItem;
import org.mustangproject.validator.ZUGFeRDValidator;

import java.util.ArrayList;
import java.util.List;

/** Validates a finished ZUGFeRD file: PDF/A conformance, XML schema and the EN 16931 business rules. */
public final class InvoiceValidator {
    private InvoiceValidator() {
    }

    /**
     * Outcome of a validation run.
     *
     * @param valid    true if the file passed all checks
     * @param problems errors found, empty if valid
     * @param report   full validation report as XML
     */
    public record Result(boolean valid, List<String> problems, String report) {
        /**
         * Creates the result with an unmodifiable list of problems.
         *
         * @param valid    true if the file passed all checks
         * @param problems errors found, empty if valid
         * @param report   full validation report as XML
         */
        public Result {
            problems = List.copyOf(problems);
        }
    }

    /**
     * Validates a ZUGFeRD file.
     *
     * @param pdf      content of the file
     * @param fileName name used in the report
     * @return the validation result with all errors found
     */
    public static Result validate(byte[] pdf, String fileName) {
        try (LibraryClassLoaderScope scope = LibraryClassLoaderScope.enter()) {
            return runValidation(pdf, fileName);
        }
    }

    private static Result runValidation(byte[] pdf, String fileName) {
        ZUGFeRDValidator validator = new ZUGFeRDValidator();
        validator.disableNotices();
        String report = validator.validate(pdf, fileName);
        List<String> problems = new ArrayList<>();
        for (ValidationResultItem item : validator.getContext().getResults()) {
            ESeverity severity = item.getSeverity();
            if (severity == ESeverity.error || severity == ESeverity.fatal || severity == ESeverity.exception) {
                problems.add(item.getMessage());
            }
        }
        return new Result(validator.wasCompletelyValid(), problems, report);
    }
}
