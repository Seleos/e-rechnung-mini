package einvoice.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests loading logos for the preview in the settings. */
class LogoImageTest {
    @Test
    void svgIsRenderedAtTheRequestedHeight(@TempDir Path dir) throws IOException {
        Path svg = dir.resolve("logo.svg");
        try (InputStream in = LogoImageTest.class.getResourceAsStream("/einvoice/sample-logo.svg")) {
            Files.copy(in, svg);
        }

        BufferedImage image = LogoImage.load(svg, 120);

        assertEquals(120, image.getHeight());
        assertTrue(image.getWidth() > image.getHeight(), "the sample logo is wider than high");
    }

    @Test
    void bitmapKeepsItsSize(@TempDir Path dir) throws IOException {
        Path png = dir.resolve("logo.png");
        ImageIO.write(new BufferedImage(40, 20, BufferedImage.TYPE_INT_ARGB), "png", png.toFile());

        BufferedImage image = LogoImage.load(png, 120);

        assertEquals(40, image.getWidth());
        assertEquals(20, image.getHeight());
    }

    @Test
    void fileThatIsNoImageIsRejected(@TempDir Path dir) throws IOException {
        Path text = Files.writeString(dir.resolve("logo.png"), "not an image");

        assertThrows(IOException.class, () -> LogoImage.load(text, 120));
    }
}
