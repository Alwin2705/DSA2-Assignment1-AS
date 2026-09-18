package utils;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.*;


public class LeafRenderer {


    private static final Color RECT_COLOR      = Color.BLUE;
    private static final Color RECT_SELECTED   = Color.RED;
    private static final Color RECT_DIM        = Color.color(0.5, 0.5, 0.5, 0.6);
    private static final Color LABEL_BG        = Color.color(0.0, 0.0, 0.8, 0.75);
    private static final Color LABEL_TEXT      = Color.WHITE;
    private static final Color PATH_COLOR      = Color.YELLOW;
    private static final Color PATH_DONE_COLOR = Color.color(1.0, 0.85, 0.0, 0.8);
    private static final Color HIGHLIGHT_COLOR = Color.CYAN;


    public static WritableImage drawClustersOnImage(Image base,
                                                    List<LeafCluster> clusters,
                                                    boolean showLabels) {
        Canvas canvas = baseCanvas(base);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        drawBaseImage(gc, base);
        drawClusterBoxes(gc, clusters, showLabels, RECT_COLOR);
        return snapshot(canvas);
    }


    public static WritableImage highlightSingleCluster(Image base,
                                                       List<LeafCluster> clusters,
                                                       LeafCluster selected,
                                                       boolean showLabels) {
        Canvas canvas = baseCanvas(base);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        drawBaseImage(gc, base);

        for (LeafCluster c : clusters) {
            if (c.equals(selected)) {
                gc.setStroke(RECT_SELECTED);
                gc.setLineWidth(2.5);
            } else {
                gc.setStroke(RECT_DIM);
                gc.setLineWidth(1.0);
            }
            gc.strokeRect(c.getMinX(), c.getMinY(), c.getWidth(), c.getHeight());

            if (showLabels) drawLabel(gc, c, c.equals(selected) ? RECT_SELECTED : RECT_DIM);
        }

        return snapshot(canvas);
    }


    public static WritableImage highlightClusterPixelsOnBW(Image bwBase,
                                                           LeafCluster cluster) {

        int w = (int) bwBase.getWidth();
        int h = (int) bwBase.getHeight();
        WritableImage out = new WritableImage(w, h);
        PixelWriter   pw  = out.getPixelWriter();


        out.getPixelWriter();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pw.setColor(x, y, bwBase.getPixelReader().getColor(x, y));
            }
        }


        for (int[] pix : cluster.getPixels()) {
            pw.setColor(pix[0], pix[1], HIGHLIGHT_COLOR);
        }

        return out;
    }

    public static WritableImage drawRandomClusterColors(Image bwBase,
                                                        List<LeafCluster> clusters) {
        int w = (int) bwBase.getWidth();
        int h = (int) bwBase.getHeight();
        WritableImage out = new WritableImage(w, h);
        PixelWriter   pw  = out.getPixelWriter();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pw.setColor(x, y, bwBase.getPixelReader().getColor(x, y));
            }
        }

        Random rng = new Random(42);

        for (LeafCluster c : clusters) {
            Color col = Color.color(
                    0.3 + rng.nextDouble() * 0.7,
                    0.3 + rng.nextDouble() * 0.7,
                    0.3 + rng.nextDouble() * 0.7);
            for (int[] pix : c.getPixels()) {
                pw.setColor(pix[0], pix[1], col);
            }
        }

        return out;
    }

    public static List<LeafCluster> computeNearestNeighbourPath(List<LeafCluster> clusters,
                                                                LeafCluster startCluster) {
        List<LeafCluster> remaining = new ArrayList<>(clusters);
        List<LeafCluster> path      = new ArrayList<>();

        LeafCluster current = (startCluster != null && remaining.contains(startCluster))
                ? startCluster
                : remaining.get(0);

        path.add(current);
        remaining.remove(current);

        while (!remaining.isEmpty()) {
            double      best = Double.MAX_VALUE;
            LeafCluster next = null;
            double cx = centerX(current);
            double cy = centerY(current);

            for (LeafCluster c : remaining) {
                double d = dist(cx, cy, centerX(c), centerY(c));
                if (d < best) { best = d; next = c; }
            }

            path.add(next);
            remaining.remove(next);
            current = next;
        }

        return path;
    }

    public static List<LeafCluster> computeNearestNeighbourPath(List<LeafCluster> clusters) {
        LeafCluster start = clusters.stream()
                .filter(c -> c.getRank() == 1)
                .findFirst()
                .orElse(clusters.isEmpty() ? null : clusters.get(0));
        return computeNearestNeighbourPath(clusters, start);
    }

    public static WritableImage drawPathStep(WritableImage currentFrame,
                                             LeafCluster a,
                                             LeafCluster b,
                                             List<LeafCluster> allClusters,
                                             boolean showLabels) {

        Canvas canvas = new Canvas(currentFrame.getWidth(), currentFrame.getHeight());
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.drawImage(currentFrame, 0, 0);

        gc.setStroke(PATH_COLOR);
        gc.setLineWidth(2.0);
        gc.strokeLine(centerX(a), centerY(a), centerX(b), centerY(b));

        gc.setStroke(Color.YELLOW);
        gc.setLineWidth(2.5);
        gc.strokeRect(b.getMinX(), b.getMinY(), b.getWidth(), b.getHeight());

        return snapshot(canvas);
    }

    public static WritableImage drawTspPath(Image base,
                                            List<LeafCluster> clusters,
                                            LeafCluster start,
                                            boolean showLabels) {
        Canvas canvas = baseCanvas(base);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        drawBaseImage(gc, base);
        drawClusterBoxes(gc, clusters, showLabels, RECT_COLOR);

        List<LeafCluster> path = computeNearestNeighbourPath(clusters, start);

        gc.setStroke(PATH_DONE_COLOR);
        gc.setLineWidth(2.0);
        for (int i = 0; i < path.size() - 1; i++) {
            gc.strokeLine(centerX(path.get(i)), centerY(path.get(i)),
                    centerX(path.get(i + 1)), centerY(path.get(i + 1)));
        }

        return snapshot(canvas);
    }

    public static void drawBoxesOnGC(GraphicsContext gc,
                                     List<LeafCluster> clusters,
                                     boolean showLabels) {
        drawClusterBoxes(gc, clusters, showLabels, RECT_COLOR);
    }

    public static void drawSegmentOnGC(GraphicsContext gc,
                                       LeafCluster a,
                                       LeafCluster b) {
        gc.setStroke(PATH_COLOR);
        gc.setLineWidth(2.0);
        gc.strokeLine(centerX(a), centerY(a), centerX(b), centerY(b));
    }



    private static void drawClusterBoxes(GraphicsContext gc,
                                         List<LeafCluster> clusters,
                                         boolean showLabels,
                                         Color stroke) {
        gc.setStroke(stroke);
        gc.setLineWidth(1.5);

        for (LeafCluster c : clusters) {
            gc.strokeRect(c.getMinX(), c.getMinY(), c.getWidth(), c.getHeight());
            if (showLabels) drawLabel(gc, c, stroke);
        }
    }

    private static void drawLabel(GraphicsContext gc, LeafCluster c, Color boxColor) {
        String text = String.valueOf(c.getRank());
        double lx   = c.getMinX();
        double ly   = c.getMinY();


        gc.setFill(LABEL_BG);
        gc.fillRoundRect(lx, ly - 15, Math.max(20, text.length() * 8 + 6), 14, 4, 4);


        gc.setFill(LABEL_TEXT);
        gc.fillText(text, lx + 3, ly - 4);
    }


    static double centerX(LeafCluster c) { return c.getMinX() + c.getWidth()  / 2.0; }
    static double centerY(LeafCluster c) { return c.getMinY() + c.getHeight() / 2.0; }

    private static double dist(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2, dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }


    private static Canvas baseCanvas(Image base) {
        return new Canvas(base.getWidth(), base.getHeight());
    }

    private static void drawBaseImage(GraphicsContext gc, Image base) {
        gc.drawImage(base, 0, 0);
    }

    static WritableImage snapshot(Canvas canvas) {
        WritableImage img = new WritableImage(
                (int) canvas.getWidth(),
                (int) canvas.getHeight());
        canvas.snapshot(new SnapshotParameters(), img);
        return img;
    }
}