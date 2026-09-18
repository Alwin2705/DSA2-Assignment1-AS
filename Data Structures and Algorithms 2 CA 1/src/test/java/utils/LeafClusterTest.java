package utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LeafClusterTest {

    // ------------------------------------------------------------
    // addPixel + size + bounds
    // ------------------------------------------------------------
    @Test
    void addPixel_updatesSizeAndBounds() {
        LeafCluster cluster = new LeafCluster(1);

        cluster.addPixel(2, 3);
        cluster.addPixel(0, 1);
        cluster.addPixel(5, 4);

        assertEquals(3, cluster.getSize());

        assertEquals(0, cluster.getMinX());
        assertEquals(1, cluster.getMinY());

        assertEquals(5, cluster.getMaxX());
        assertEquals(4, cluster.getMaxY());
    }

    // ------------------------------------------------------------
    // id
    // ------------------------------------------------------------
    @Test
    void getId_returnsCorrectId() {
        LeafCluster cluster = new LeafCluster(42);
        assertEquals(42, cluster.getId());
    }

    // ------------------------------------------------------------
    // width + height
    // ------------------------------------------------------------
    @Test
    void widthAndHeight_calculatedCorrectly() {
        LeafCluster cluster = new LeafCluster(1);

        cluster.addPixel(2, 3);
        cluster.addPixel(5, 7);

        assertEquals(4, cluster.getWidth());  // 2 → 5
        assertEquals(5, cluster.getHeight()); // 3 → 7
    }

    // ------------------------------------------------------------
    // pixels list
    // ------------------------------------------------------------
    @Test
    void getPixels_returnsAllPixels() {
        LeafCluster cluster = new LeafCluster(1);

        cluster.addPixel(1, 1);
        cluster.addPixel(2, 2);

        List<int[]> pixels = cluster.getPixels();

        assertEquals(2, pixels.size());
        assertArrayEquals(new int[]{1, 1}, pixels.get(0));
        assertArrayEquals(new int[]{2, 2}, pixels.get(1));
    }

    // ------------------------------------------------------------
    // rank
    // ------------------------------------------------------------
    @Test
    void rank_canBeSetAndRetrieved() {
        LeafCluster cluster = new LeafCluster(1);

        cluster.setRank(10);
        assertEquals(10, cluster.getRank());
    }


    // ------------------------------------------------------------
    // equals + hashCode
    // ------------------------------------------------------------
    @Test
    void equalsAndHashCode_basedOnIdOnly() {
        LeafCluster a = new LeafCluster(1);
        LeafCluster b = new LeafCluster(1);
        LeafCluster c = new LeafCluster(2);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        assertNotEquals(a, c);
    }

    // ------------------------------------------------------------
    // toString
    // ------------------------------------------------------------
    @Test
    void toString_containsKeyInfo() {
        LeafCluster cluster = new LeafCluster(5);

        cluster.addPixel(1, 2);
        cluster.addPixel(3, 4);
        cluster.setRank(2);

        String text = cluster.toString();

        assertTrue(text.contains("rank=2"));
        assertTrue(text.contains("size=2"));
        assertTrue(text.contains("bbox"));
    }
}