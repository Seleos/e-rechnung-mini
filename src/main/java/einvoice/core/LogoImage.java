package einvoice.core;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.imageio.ImageIO;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Locale;

/**
 * Prepares a company logo: as an image for the on-screen preview, and as the source the invoice layout draws.
 * Supports SVG and the common bitmap formats.
 */
public final class LogoImage {
    private static final String SVG_DATA_PREFIX = "data:image/svg+xml;base64,";
    private static final String WIDTH = "width";
    private static final String HEIGHT = "height";
    private static final int VIEW_BOX_VALUES = 4;

    private LogoImage() {
    }

    /**
     * Returns the logo as the invoice layout loads it. An SVG file without width and height has no aspect ratio
     * the PDF renderer can use; it then fits the drawing into a square and the logo comes out far too small. Such
     * a file is handed over with the size of its view box, the file itself is left unchanged.
     *
     * @param file logo file, may be null
     * @return a URI the stylesheet can load, or an empty text if there is no logo
     * @throws IOException if an SVG file cannot be read or is not well-formed
     */
    public static String layoutSource(Path file) throws IOException {
        if (file == null) {
            return "";
        }
        if (!isSvg(file)) {
            return file.toUri().toString();
        }
        Document svg = parse(file);
        Element root = svg.getDocumentElement();
        String[] viewBox = root.getAttribute("viewBox").trim().split("[\\s,]+");
        boolean sized = !root.getAttribute(WIDTH).isBlank() && !root.getAttribute(HEIGHT).isBlank();
        if (sized || viewBox.length != VIEW_BOX_VALUES) {
            return file.toUri().toString();
        }
        root.setAttribute(WIDTH, viewBox[2]);
        root.setAttribute(HEIGHT, viewBox[3]);
        return SVG_DATA_PREFIX + Base64.getEncoder().encodeToString(serialize(svg));
    }

    /**
     * Loads a logo file.
     *
     * @param file   logo file (SVG, PNG or JPEG)
     * @param height height of the returned image in pixels for SVG files; bitmaps keep their size
     * @return the image
     * @throws IOException if the file cannot be read or is not an image
     */
    public static BufferedImage load(Path file, int height) throws IOException {
        if (isSvg(file)) {
            return renderSvg(file, height);
        }
        BufferedImage image = ImageIO.read(file.toFile());
        if (image == null) {
            throw new IOException("Not an image: " + file);
        }
        return image;
    }

    private static boolean isSvg(Path file) {
        return file.toString().toLowerCase(Locale.ROOT).endsWith(".svg");
    }

    /** Reads the SVG without loading external DTDs or entities, so a logo file cannot reach other files. */
    private static Document parse(Path file) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newDefaultInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder().parse(file.toFile());
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("The SVG file cannot be read: " + file, e);
        }
    }

    private static byte[] serialize(Document svg) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            TransformerFactory.newDefaultInstance().newTransformer().transform(new DOMSource(svg),
                    new StreamResult(bytes));
        } catch (TransformerException e) {
            throw new IOException("The SVG file cannot be written", e);
        }
        return bytes.toByteArray();
    }

    private static BufferedImage renderSvg(Path file, int height) throws IOException {
        InMemoryTranscoder transcoder = new InMemoryTranscoder();
        transcoder.addTranscodingHint(ImageTranscoder.KEY_HEIGHT, (float) height);
        try (LibraryClassLoaderScope scope = LibraryClassLoaderScope.enter()) {
            transcoder.transcode(new TranscoderInput(file.toUri().toString()), null);
        } catch (TranscoderException e) {
            throw new IOException("The SVG file cannot be rendered: " + file, e);
        }
        return transcoder.image;
    }

    /** Keeps the rendered image in memory instead of encoding it into a file format. */
    private static final class InMemoryTranscoder extends ImageTranscoder {
        private BufferedImage image;

        @Override
        public BufferedImage createImage(int width, int height) {
            return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }

        @Override
        public void writeImage(BufferedImage rendered, TranscoderOutput output) {
            image = rendered;
        }
    }
}
