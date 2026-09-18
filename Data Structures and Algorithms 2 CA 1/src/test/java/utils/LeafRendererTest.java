package utils;

import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LeafRendererTest {

    // ------------------------------------------------------------
    // helper cluster
    // ------------------------------------------------------------
    private LeafCluster cluster(int id, int x, int y, int w, int h) {
        LeafCluster c = new LeafCluster(id);
        for (int i = x; i < x + w; i++) {
            for (int j = y; j < y + h; j++) {
                c.addPixel(i, j);
            }
        }
        return c;
    }

    // ------------------------------------------------------------
    // highlight pixels
    // ------------------------------------------------------------
    @Test
    void highlightClusterPixelsOnBW() {
        WritableImage img = new WritableImage(3, 3);
        LeafCluster c = new LeafCluster(1);
        c.addPixel(1, 1);

        WritableImage out = LeafRenderer.highlightClusterPixelsOnBW(img, c);

        Color pixel = out.getPixelReader().getColor(1, 1);

        assertEquals(Color.CYAN, pixel);
    }

    // ------------------------------------------------------------
    // random coloring (deterministic check)
    // ------------------------------------------------------------
    @Test
    void drawRandomClusterColors() {
        WritableImage img = new WritableImage(2, 2);

        LeafCluster c = new LeafCluster(1);
        c.addPixel(0, 0);
        c.addPixel(1, 1);

        WritableImage out =
                LeafRenderer.drawRandomClusterColors(img, List.of(c));

        assertNotNull(out);
        assertEquals(2, out.getWidth());
        assertEquals(2, out.getHeight());
    }

    // ------------------------------------------------------------
    // nearest neighbour path correctness
    // ------------------------------------------------------------
    @Test
    void computeNearestNeighbourPath() {
        LeafCluster a = cluster(1, 0, 0, 1, 1);
        LeafCluster b = cluster(2, 5, 0, 1, 1);
        LeafCluster c = cluster(3, 10, 0, 1, 1);

        List<LeafCluster> path =
                LeafRenderer.computeNearestNeighbourPath(List.of(a, b, c), a);

        assertEquals(3, path.size());
        assertEquals(a, path.get(0));
    }

    // ------------------------------------------------------------
    // overloaded path method
    // ------------------------------------------------------------
    @Test
    void testComputeNearestNeighbourPath() {
        LeafCluster a = cluster(1, 0, 0, 1, 1);
        a.setRank(1);

        LeafCluster b = cluster(2, 3, 0, 1, 1);

        List<LeafCluster> path =
                LeafRenderer.computeNearestNeighbourPath(List.of(a, b));

        assertEquals(2, path.size());
    }

    // ------------------------------------------------------------
    // center functions
    // ------------------------------------------------------------
    @Test
    void centerFunctions() {
        LeafCluster c = cluster(1, 0, 0, 2, 2);

        double cx = LeafRenderer.centerX(c);
        double cy = LeafRenderer.centerY(c);

        assertEquals(1.0, cx);
        assertEquals(1.0, cy);
    }

}