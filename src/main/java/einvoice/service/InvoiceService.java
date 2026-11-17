package einvoice.service;

import einvoice.core.InvoiceValidator;
import einvoice.core.PdfBuilder;
import einvoice.core.XmlBuilder;
import einvoice.i18n.Messages;
import einvoice.model.Company;
import einvoice.model.InvoiceData;
import einvoice.storage.DataStore;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Ties XML generation, PDF rendering, validation and storage together. */
public final class InvoiceService {
    private final DataStore store;

    /**
     * Creates the service.
     *
     * @param store data store used for logos, templates, output folders and records
     */
    public InvoiceService(DataStore store) {
        this.store = store;
    }

    /** Thrown when the generated file does not pass validation; nothing is saved in that case. */
    public static class InvalidInvoiceException extends Exception {
        private final InvoiceValidator.Result result;

        /**
         * Creates the exception.
         *
         * @param result validation result that lists the problems
         */
        public InvalidInvoiceException(InvoiceValidator.Result result) {
            super(Messages.get("service.invalidInvoice"));
            this.result = result;
        }

        /**
         * Returns the failed validation.
         *
         * @return validation result that lists the problems
         */
        public InvoiceValidator.Result result() {
            return result;
        }
    }

    /**
     * Renders the visible PDF only, used for the live preview.
     *
     * @param company issuing company
     * @param invoice invoice data that passed the input check
     * @return the PDF content without embedded XML
     * @throws Exception if generation fails
     */
    public byte[] preview(Company company, InvoiceData invoice) throws Exception {
        byte[] xml = XmlBuilder.xml(company, invoice);
        return PdfBuilder.renderLayout(company, invoice, xml, store.logoFile(company), store.templateFile(company));
    }

    /**
     * Builds the complete ZUGFeRD file in memory, not yet validated or saved.
     *
     * @param company issuing company
     * @param invoice invoice data that passed the input check
     * @return the PDF content with embedded XML
     * @throws Exception if generation fails
     */
    public byte[] build(Company company, InvoiceData invoice) throws Exception {
        byte[] xml = XmlBuilder.xml(company, invoice);
        byte[] layout = PdfBuilder.renderLayout(company, invoice, xml,
                store.logoFile(company), store.templateFile(company));
        return PdfBuilder.embedXml(company, layout, xml);
    }

    /**
     * Builds, validates and saves the invoice. An existing file is never overwritten.
     *
     * @param company issuing company
     * @param invoice invoice data that passed the input check; totals and file name are filled in
     * @return path of the saved PDF
     * @throws InvalidInvoiceException          if the generated file fails validation; nothing is saved
     * @throws java.nio.file.FileAlreadyExistsException if an invoice with this number already exists
     * @throws Exception                        if generation or saving fails
     */
    public Path create(Company company, InvoiceData invoice) throws Exception {
        String fileName = invoice.getNumber() + ".pdf";
        Path folder = store.outputDir(company, invoice.getDate().getYear());
        Path target = folder.resolve(fileName);
        if (Files.exists(target) || store.hasRecord(company, invoice.getNumber())) {
            throw new FileAlreadyExistsException(invoice.getNumber());
        }
        byte[] pdf = build(company, invoice);
        InvoiceValidator.Result result = InvoiceValidator.validate(pdf, fileName);
        if (!result.valid()) {
            throw new InvalidInvoiceException(result);
        }
        Files.createDirectories(folder);
        Files.write(target, pdf, StandardOpenOption.CREATE_NEW);
        XmlBuilder.Totals totals = XmlBuilder.totals(company, invoice);
        invoice.setNet(totals.net());
        invoice.setTax(totals.tax());
        invoice.setGross(totals.gross());
        // Only the file name is stored, so the record also works on another computer with the same folders.
        invoice.setFile(fileName);
        try {
            store.saveRecord(company, invoice);
        } catch (RuntimeException e) {
            // Without its record the invoice would block the number but not appear in the list.
            Files.deleteIfExists(target);
            throw e;
        }
        return target;
    }

    /**
     * Finds the PDF of a created invoice.
     *
     * @param company issuing company
     * @param record  record of the invoice
     * @return the file in the output folder of the invoice year; records written by early versions hold an
     *         absolute path, which is used as it is
     */
    public Path file(Company company, InvoiceData record) {
        return store.outputDir(company, record.getDate().getYear()).resolve(record.getFile());
    }
}
