package utils;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ImageProcessorTest {

    // ------------------------------------------------------------
    // Helper image (3x3 pattern)
    // ------------------------------------------------------------
    private Image createTestImage() {
        WritableImage img = new WritableImage(3, 3);
        var pw = img.getPixelWriter();

        pw.setColor(0, 0, Color.WHITE);
        pw.setColor(1, 0, Color.WHITE);
        pw.setColor(1, 1, Color.WHITE);

        pw.setColor(0, 1, Color.BLACK);
        pw.setColor(2, 2, Color.BLACK);

        return img;
    }



    // ------------------------------------------------------------
    // dimensions
    // ------------------------------------------------------------
    @Test
    void testDimensions() {
        ImageProcessor proc = ImageProcessor.fromBlackWhiteImage(createTestImage());

        assertEquals(3, proc.getWidth());
        assertEquals(3, proc.getHeight());
    }

    // ------------------------------------------------------------
    // computeClusters
    // ------------------------------------------------------------
    @Test
    void computeClusters() {
        ImageProcessor proc = ImageProcessor.fromBlackWhiteImage(createTestImage());

        List<LeafCluster> clusters = proc.computeClusters(1, 10);

        assertNotNull(clusters);
        assertFalse(clusters.isEmpty());

        for (LeafCluster c : clusters) {
            assertTrue(c.getSize() > 0);
        }
    }

    // ------------------------------------------------------------
    // IQR filter
    // ------------------------------------------------------------
    @Test
    void applyIQRFilter() {
        LeafCluster small = new LeafCluster(1);
        small.addPixel(0, 0);

        LeafCluster medium = new LeafCluster(2);
        for (int i = 0; i < 10; i++) medium.addPixel(i, 0);

        LeafCluster large = new LeafCluster(3);
        for (int i = 0; i < 1000; i++) large.addPixel(i, 0);

        List<LeafCluster> input = List.of(small, medium, large);

        List<LeafCluster> filtered = ImageProcessor.applyIQRFilter(input);

        assertNotNull(filtered);
        assertTrue(filtered.size() <= input.size());
        assertTrue(filtered.stream().allMatch(c -> c != null));
    }

    // ------------------------------------------------------------
    // IQR edge cases
    // ------------------------------------------------------------
    @Test
    void applyIQRFilterEdgeCases() {
        assertTrue(ImageProcessor.applyIQRFilter(List.of()).isEmpty());

        List<LeafCluster> single =
                List.of(new LeafCluster(1));

        assertEquals(1, ImageProcessor.applyIQRFilter(single).size());
    }

    // ------------------------------------------------------------
    // toBlackWhite
    // ------------------------------------------------------------
    @Test
    void toBlackWhite() {
        WritableImage img = new WritableImage(1, 1);
        img.getPixelWriter().setColor(0, 0, Color.GREEN);

        WritableImage result = ImageProcessor.toBlackWhite(
                img,
                List.of(Color.GREEN),
                0.5
        );

        Color out = result.getPixelReader().getColor(0, 0);

        assertNotNull(out);
        assertTrue(out.equals(Color.WHITE) || out.equals(Color.BLACK));
    }


}