package einvoice.core;

import einvoice.AppInfo;
import einvoice.i18n.InvoiceTexts;
import einvoice.model.Company;
import einvoice.model.InvoiceData;
import net.sf.saxon.TransformerFactoryImpl;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.FopFactoryBuilder;
import org.apache.fop.apps.MimeConstants;
import org.apache.fop.configuration.DefaultConfigurationBuilder;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.mustangproject.ZUGFeRD.IZUGFeRDExporter;
import org.mustangproject.ZUGFeRD.Profiles;
import org.mustangproject.ZUGFeRD.ZUGFeRDExporterFromPDFA;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/**
 * Renders the invoice PDF. The visible page is produced from the invoice XML via XSLT and Apache FOP,
 * then the same XML is embedded, which makes the file a ZUGFeRD invoice (PDF/A-3).
 */
public final class PdfBuilder {
    private static final String DEFAULT_TEMPLATE = "/einvoice/template.xsl";
    /** Texts of these groups are handed to the stylesheet as parameters of the same name. */
    private static final String[] STYLESHEET_TEXT_PREFIXES = {"label.", "unit."};
    private static final String[] FONT_FILES = {"LiberationSans-Regular.ttf", "LiberationSans-Bold.ttf"};

    /** FOP configuration that registers the bundled fonts; the placeholders are replaced by file locations. */
    private static final String FOP_CONFIG = """
            <fop version="1.0">
              <renderers>
                <renderer mime="application/pdf">
                  <fonts>
                    <font kerning="yes" embed-url="{regular}">
                      <font-triplet name="Liberation Sans" style="normal" weight="normal"/>
                    </font>
                    <font kerning="yes" embed-url="{bold}">
                      <font-triplet name="Liberation Sans" style="normal" weight="bold"/>
                    </font>
                  </fonts>
                </renderer>
              </renderers>
            </fop>
            """;

    private static final TransformerFactory TRANSFORMERS = new TransformerFactoryImpl();
    private static final Object RENDER_LOCK = new Object();
    private static FopFactory fopFactory;
    private static Templates defaultTemplate;

    private PdfBuilder() {
    }

    /**
     * Renders the visible invoice as PDF/A-3 without the embedded XML.
     *
     * @param company        issuing company, source of the footer details
     * @param invoice        invoice data, source of the service period
     * @param xml            invoice XML the page is rendered from
     * @param logo           logo file or null
     * @param customTemplate company specific stylesheet or null for the built-in layout
     * @return the PDF content
     * @throws Exception if the stylesheet is invalid or rendering fails
     */
    public static byte[] renderLayout(Company company, InvoiceData invoice, byte[] xml, Path logo, Path customTemplate)
            throws Exception {
        // FOP sets up its fonts on first use and is not safe for concurrent rendering with one factory.
        synchronized (RENDER_LOCK) {
            try (LibraryClassLoaderScope scope = LibraryClassLoaderScope.enter()) {
                Transformer transformer = template(customTemplate).newTransformer();
                transformer.setParameter("logo", logo == null ? "" : logo.toUri().toString());
                transformer.setParameter("managingDirector", text(company.getManagingDirector()));
                transformer.setParameter("registerCourt", text(company.getRegisterCourt()));
                transformer.setParameter("registerNumber", text(company.getRegisterNumber()));
                transformer.setParameter("bankName", text(company.getBankName()));
                transformer.setParameter("phone", text(company.getPhone()));
                transformer.setParameter("email", text(company.getEmail()));
                transformer.setParameter("web", text(company.getWeb()));
                transformer.setParameter("contact", text(company.getContact()));
                transformer.setParameter("footerExtra", text(company.getFooterExtra()));
                InvoiceTexts texts = invoiceTexts(invoice);
                transformer.setParameter("language", texts.language().getCode());
                for (String key : texts.keys(STYLESHEET_TEXT_PREFIXES)) {
                    transformer.setParameter(key, texts.get(key));
                }
                transformer.setParameter("period", texts.period(invoice.getServiceFrom(), invoice.getServiceTo()));
                transformer.setParameter("customerCountry", countryName(invoice, texts.locale()));

                FOUserAgent userAgent = fopFactory().newFOUserAgent();
                userAgent.getRendererOptions().put("pdf-a-mode", "PDF/A-3b");
                userAgent.setTitle(texts.get("label.invoiceTitle") + " " + invoice.getNumber());
                userAgent.setAuthor(company.getName());
                userAgent.setCreator(AppInfo.name());
                userAgent.setProducer(AppInfo.name());

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                Fop fop = fopFactory().newFop(MimeConstants.MIME_PDF, userAgent, out);
                transformer.transform(new StreamSource(new ByteArrayInputStream(xml)),
                        new SAXResult(fop.getDefaultHandler()));
                return out.toByteArray();
            }
        }
    }

    /**
     * Embeds the invoice XML into the rendered PDF.
     *
     * @param company   issuing company, recorded as creator of the file
     * @param layoutPdf visible invoice as produced by {@link #renderLayout}
     * @param xml       invoice XML to embed
     * @return the finished ZUGFeRD file
     * @throws IOException if the PDF cannot be read or written
     */
    public static byte[] embedXml(Company company, byte[] layoutPdf, byte[] xml) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (LibraryClassLoaderScope scope = LibraryClassLoaderScope.enter();
             IZUGFeRDExporter exporter = new ZUGFeRDExporterFromPDFA().load(layoutPdf)) {
            exporter.setProducer(AppInfo.name())
                    .setCreator(company.getName())
                    .setProfile(Profiles.getByName(XmlBuilder.PROFILE))
                    .setXML(xml);
            exporter.export(out);
        }
        return out.toByteArray();
    }

    /**
     * Renders the first page as an image for the on-screen preview.
     *
     * @param pdf PDF content
     * @param dpi resolution of the image
     * @return the rendered page
     * @throws IOException if the PDF cannot be read
     */
    public static BufferedImage renderFirstPage(byte[] pdf, float dpi) throws IOException {
        try (LibraryClassLoaderScope scope = LibraryClassLoaderScope.enter();
             PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFRenderer(document).renderImageWithDPI(0, dpi);
        }
    }

    private static Templates template(Path customTemplate) throws Exception {
        if (customTemplate != null && Files.isRegularFile(customTemplate)) {
            return TRANSFORMERS.newTemplates(new StreamSource(customTemplate.toFile()));
        }
        synchronized (RENDER_LOCK) {
            if (defaultTemplate == null) {
                try (InputStream in = PdfBuilder.class.getResourceAsStream(DEFAULT_TEMPLATE)) {
                    defaultTemplate = TRANSFORMERS.newTemplates(new StreamSource(in));
                }
            }
            return defaultTemplate;
        }
    }

    private static FopFactory fopFactory() throws Exception {
        synchronized (RENDER_LOCK) {
            if (fopFactory == null) {
                fopFactory = createFopFactory();
            }
            return fopFactory;
        }
    }

    private static FopFactory createFopFactory() throws Exception {
        Path fontDir = extractFonts();
        String config = FOP_CONFIG
                .replace("{regular}", fontDir.resolve(FONT_FILES[0]).toUri().toString())
                .replace("{bold}", fontDir.resolve(FONT_FILES[1]).toUri().toString());
        FopFactoryBuilder builder = new FopFactoryBuilder(new File(".").toURI());
        builder.setConfiguration(new DefaultConfigurationBuilder()
                .build(new ByteArrayInputStream(config.getBytes(StandardCharsets.UTF_8))));
        return builder.build();
    }

    /** FOP needs font files on disk, so the bundled fonts are copied out of the application once. */
    private static Path extractFonts() throws IOException {
        Path fontDir = Files.createTempDirectory("einvoice-fonts");
        fontDir.toFile().deleteOnExit();
        for (String fontFile : FONT_FILES) {
            Path target = fontDir.resolve(fontFile);
            try (InputStream in = PdfBuilder.class.getResourceAsStream("/einvoice/fonts/" + fontFile)) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            target.toFile().deleteOnExit();
        }
        return fontDir;
    }

    private static InvoiceTexts invoiceTexts(InvoiceData invoice) {
        return InvoiceTexts.of(invoice.getCustomer() == null ? null : invoice.getCustomer().getInvoiceLanguage());
    }

    /** Country name for the address window in the invoice language; empty for domestic customers. */
    private static String countryName(InvoiceData invoice, Locale locale) {
        if (invoice.getCustomer() == null || invoice.getCustomer().getCountry() == null) {
            return "";
        }
        String code = invoice.getCustomer().getCountry().trim().toUpperCase();
        if (code.isEmpty() || code.equals("DE")) {
            return "";
        }
        return Locale.of("", code).getDisplayCountry(locale);
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }
}
