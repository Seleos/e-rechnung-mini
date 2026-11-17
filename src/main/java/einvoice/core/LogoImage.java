package einvoice.core;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

/** Loads a company logo as an image for the on-screen preview. Supports SVG and the common bitmap formats. */
public final class LogoImage {
    private LogoImage() {
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
        if (file.toString().toLowerCase(Locale.ROOT).endsWith(".svg")) {
            return renderSvg(file, height);
        }
        BufferedImage image = ImageIO.read(file.toFile());
        if (image == null) {
            throw new IOException("Not an image: " + file);
        }
        return image;
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
