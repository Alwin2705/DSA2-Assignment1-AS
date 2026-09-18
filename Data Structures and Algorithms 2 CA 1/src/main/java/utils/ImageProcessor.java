package utils;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;

import java.util.*;

public class ImageProcessor {


    private final int width;
    private final int height;
    private final boolean[][] white;  // true = leaf pixel
    private final UnionFind   uf;


    public ImageProcessor(int width, int height) {
        this.width  = width;
        this.height = height;
        this.white  = new boolean[height][width];
        this.uf     = new UnionFind(width * height);
    }



    public static WritableImage toBlackWhite(Image src,
                                             List<Color> targetColors,
                                             double maxDist,
                                             double hueTolDeg,
                                             double satMin,
                                             double briMin) {
        int w = (int) src.getWidth();
        int h = (int) src.getHeight();
        WritableImage bw = new WritableImage(w, h);
        PixelReader   pr = src.getPixelReader();
        PixelWriter   pw = bw.getPixelWriter();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Color c = pr.getColor(x, y);
                boolean leaf = matchesAny(c, targetColors, maxDist, hueTolDeg, satMin, briMin);
                pw.setColor(x, y, leaf ? Color.WHITE : Color.BLACK);
            }
        }
        return bw;
    }


    public static WritableImage toBlackWhite(Image src,
                                             List<Color> targetColors,
                                             double maxDist) {
        return toBlackWhite(src, targetColors, maxDist, 30.0, 0.15, 0.15);
    }


    private static boolean matchesAny(Color c,
                                      List<Color> targets,
                                      double maxDist,
                                      double hueTolDeg,
                                      double satMin,
                                      double briMin) {

        double sat = c.getSaturation();
        double bri = c.getBrightness();

        if (sat < satMin || bri < briMin) return false;

        double hue = c.getHue();

        for (Color t : targets) {

            double dh = Math.min(
                    Math.abs(hue - t.getHue()),
                    360.0 - Math.abs(hue - t.getHue()));

            if (dh > hueTolDeg) continue;

            double ds = Math.abs(sat - t.getSaturation());
            double db = Math.abs(bri - t.getBrightness());


            double dist = (dh / 360.0) + ds + db;
            if (dist < maxDist * 3) return true;
        }
        return false;
    }


    public static ImageProcessor fromBlackWhiteImage(Image bwImage) {
        int w  = (int) bwImage.getWidth();
        int h  = (int) bwImage.getHeight();
        ImageProcessor proc = new ImageProcessor(w, h);
        PixelReader    pr   = bwImage.getPixelReader();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                proc.white[y][x] = pr.getColor(x, y).getBrightness() > 0.5;
            }
        }
        return proc;
    }


    public List<LeafCluster> computeClusters(int minSize, int maxSize) {

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!white[y][x]) continue;
                int idx = index(x, y);

                if (x + 1 < width  && white[y][x + 1]) uf.union(idx, index(x + 1, y));
                if (y + 1 < height && white[y + 1][x]) uf.union(idx, index(x,     y + 1));
            }
        }

        Map<Integer, LeafCluster> map = new HashMap<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!white[y][x]) continue;
                int root = uf.find(index(x, y));
                map.computeIfAbsent(root, LeafCluster::new).addPixel(x, y);
            }
        }

        List<LeafCluster> result = new ArrayList<>();
        for (LeafCluster c : map.values()) {
            int s = c.getSize();
            if (s >= minSize && (maxSize <= 0 || s <= maxSize)) {
                result.add(c);
            }
        }

        MergeSort.sortBySizeDesc(result);
        for (int i = 0; i < result.size(); i++) {
            result.get(i).setRank(i + 1);
        }

        return result;
    }

    public static List<LeafCluster> applyIQRFilter(List<LeafCluster> clusters) {
        if (clusters == null || clusters.size() < 4) return clusters;

        List<Integer> sizes = clusters.stream()
                .map(LeafCluster::getSize)
                .sorted()
                .toList();

        double q1 = percentile(sizes, 25);
        double q3 = percentile(sizes, 75);
        double iqr = q3 - q1;

        double lower = q1 - 1.5 * iqr;
        double upper = q3 + 1.5 * iqr;

        List<LeafCluster> filtered = new ArrayList<>();
        for (LeafCluster c : clusters) {
            int s = c.getSize();
            if (s >= lower && s <= upper) filtered.add(c);
        }
        return filtered;
    }

    private static double percentile(List<Integer> sorted, double p) {
        if (sorted.isEmpty()) return 0;
        double index = (p / 100.0) * (sorted.size() - 1);
        int lo = (int) Math.floor(index);
        int hi = (int) Math.ceil(index);
        if (lo == hi) return sorted.get(lo);
        double w = index - lo;
        return sorted.get(lo) * (1 - w) + sorted.get(hi) * w;
    }


    public int getWidth()          { return width;  }
    public int getHeight()         { return height; }
    public boolean[][] getWhiteMask() { return white; }



    private int index(int x, int y) {
        return y * width + x;
    }
}