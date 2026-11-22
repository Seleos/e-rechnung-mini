package einvoice.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests loading logos for the preview in the settings and preparing them for the invoice layout. */
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

    @Test
    void svgWithoutSizeIsHandedToTheLayoutWithTheSizeOfItsViewBox(@TempDir Path dir) throws IOException {
        Path svg = Files.writeString(dir.resolve("logo.svg"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 132.52 40"><rect width="132.52" height="40"/></svg>
                """);

        String source = LogoImage.layoutSource(svg);

        assertTrue(source.startsWith("data:image/svg+xml;base64,"), source);
        String content = new String(Base64.getDecoder().decode(source.substring(source.indexOf(',') + 1)),
                StandardCharsets.UTF_8);
        assertTrue(content.contains("width=\"132.52\""), content);
        assertTrue(content.contains("height=\"40\""), content);
        assertTrue(Files.readString(svg).startsWith("<?xml"), "the logo file stays unchanged");
    }

    @Test
    void sizedSvgBitmapsAndSvgWithoutViewBoxAreUsedAsTheyAre(@TempDir Path dir) throws IOException {
        Path sized = dir.resolve("sized.svg");
        try (InputStream in = LogoImageTest.class.getResourceAsStream("/einvoice/sample-logo.svg")) {
            Files.copy(in, sized);
        }
        Path withoutViewBox = Files.writeString(dir.resolve("plain.svg"),
                "<svg xmlns=\"http://www.w3.org/2000/svg\"><rect width=\"10\" height=\"10\"/></svg>");
        Path bitmap = dir.resolve("logo.png");

        assertEquals(sized.toUri().toString(), LogoImage.layoutSource(sized));
        assertEquals(withoutViewBox.toUri().toString(), LogoImage.layoutSource(withoutViewBox));
        assertEquals(bitmap.toUri().toString(), LogoImage.layoutSource(bitmap));
        assertEquals("", LogoImage.layoutSource(null));
    }

    @Test
    void svgThatIsNotWellFormedIsRejected(@TempDir Path dir) throws IOException {
        Path broken = Files.writeString(dir.resolve("logo.svg"), "<svg");

        assertThrows(IOException.class, () -> LogoImage.layoutSource(broken));
    }
}
