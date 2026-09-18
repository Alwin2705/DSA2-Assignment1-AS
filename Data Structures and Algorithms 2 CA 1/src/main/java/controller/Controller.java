package controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import utils.ImageProcessor;
import utils.LeafCluster;
import utils.LeafRenderer;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class Controller {



    @FXML
    private ImageView originalView;
    @FXML private ImageView bwView;

    @FXML private Slider    distanceSlider;
    @FXML private Slider    hueToleranceSlider;
    @FXML private Slider    satMinSlider;
    @FXML private Slider    briMinSlider;
    @FXML private TextField minSizeField;
    @FXML private TextField maxSizeField;

    @FXML private Label statusLabel;


    @FXML private Label clusterCountLabel;
    @FXML private Label selectedInfoLabel;
    @FXML private Label imageSizeLabel;


    @FXML private CheckMenuItem showLabelsMenuItem;
    @FXML private CheckMenuItem iqrFilterMenuItem;


    @FXML private CheckBox      iqrFilterCheckBox;
    @FXML private ListView<Color> colorListView;



    private Image         originalImage;
    private WritableImage bwImage;
    private WritableImage annotatedImage;

    private ImageProcessor    processor;
    private List<LeafCluster> clusters = new ArrayList<>();

    private final List<Color> targetColors = new ArrayList<>();


    private boolean samplingMode = false;


    private LeafCluster tspStartCluster = null;



    @FXML
    public void initialize() {
        targetColors.add(Color.ORANGE);
        targetColors.add(Color.GOLDENROD);
        targetColors.add(Color.RED);
        targetColors.add(Color.SADDLEBROWN);
        refreshColorList();

        status("Ready. Load an image to begin.");

        originalView.setOnMouseClicked(this::handleOriginalViewClick);
        originalView.setOnMouseMoved(this::handleMouseHover);
        bwView.setOnMouseClicked(this::handleBwViewClick);
    }



    @FXML
    public void onLoadImage(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Autumn Leaves Image");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.bmp"));

        File file = chooser.showOpenDialog(originalView.getScene().getWindow());
        if (file == null) return;

        try {
            originalImage = SwingFXUtils.toFXImage(ImageIO.read(file), null);
            originalView.setImage(originalImage);
            resetState();

            int w = (int) originalImage.getWidth();
            int h = (int) originalImage.getHeight();
            imageSizeLabel.setText("Image size: " + w + " x " + h + " px");
            status("Loaded: " + file.getName() + "  (" + w + " x " + h + ")");
        } catch (IOException e) {
            showAlert("Image load failed", e.getMessage());
        }
    }

    @FXML
    public void onSampleColor(ActionEvent event) {
        if (originalImage == null) { status("Load an image first."); return; }
        samplingMode = true;
        status("Sampling mode: click on a leaf in the original image to pick its colour.");
    }

    @FXML
    public void onRemoveColor(ActionEvent event) {
        Color sel = colorListView.getSelectionModel().getSelectedItem();
        if (sel != null) {
            targetColors.remove(sel);
            refreshColorList();
        }
    }

    @FXML
    public void onClearColors(ActionEvent event) {
        targetColors.clear();
        refreshColorList();
        status("Colour list cleared.");
    }

    private void refreshColorList() {
        colorListView.getItems().setAll(targetColors);
        colorListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Color c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) {
                    setText(null);
                    setStyle("");
                } else {
                    String hex = toHex(c);
                    setText("  " + hex);
                    setStyle("-fx-background-color:" + hex + ";"
                            + "-fx-text-fill:" + (c.getBrightness() > 0.5 ? "#000" : "#fff") + ";");
                }
            }
        });
    }

    private String toHex(Color c) {
        return String.format("#%02X%02X%02X",
                (int)(c.getRed()   * 255),
                (int)(c.getGreen() * 255),
                (int)(c.getBlue()  * 255));
    }


    @FXML
    public void onConvertToBW(ActionEvent event) {
        if (originalImage == null) { status("Load an image first."); return; }
        if (targetColors.isEmpty()) { status("Add at least one target colour first."); return; }

        double dist   = distanceSlider.getValue();
        double hueTol = hueToleranceSlider != null ? hueToleranceSlider.getValue() : 30.0;
        double satMin = satMinSlider       != null ? satMinSlider.getValue()       : 0.15;
        double briMin = briMinSlider       != null ? briMinSlider.getValue()       : 0.15;

        bwImage = ImageProcessor.toBlackWhite(originalImage, targetColors, dist, hueTol, satMin, briMin);
        bwView.setImage(bwImage);

        clusters.clear();
        processor     = null;
        annotatedImage = null;
        resetStatsLabels();

        status("B/W conversion done  |  dist=" + String.format("%.2f", dist)
                + "  hue=" + String.format("%.0f°", hueTol));
    }


    @FXML
    public void onDetectLeaves(ActionEvent event) {
        if (bwImage == null) { status("Convert to B/W first."); return; }

        int minSize = parse(minSizeField.getText(), 20);
        int maxSize = parse(maxSizeField.getText(), 0);

        processor = ImageProcessor.fromBlackWhiteImage(bwImage);
        clusters  = processor.computeClusters(minSize, maxSize);

        boolean iqrEnabled = (iqrFilterMenuItem  != null && iqrFilterMenuItem.isSelected())
                || (iqrFilterCheckBox   != null && iqrFilterCheckBox.isSelected());
        if (iqrEnabled) {
            clusters = ImageProcessor.applyIQRFilter(clusters);
            for (int i = 0; i < clusters.size(); i++) clusters.get(i).setRank(i + 1);
        }

        boolean labels = showLabelsMenuItem == null || showLabelsMenuItem.isSelected();
        annotatedImage = LeafRenderer.drawClustersOnImage(originalImage, clusters, labels);
        originalView.setImage(annotatedImage);

        updateClusterCountLabel();
        selectedInfoLabel.setText("Selected: —");
        status("Detected " + clusters.size() + " leaf cluster(s).");
    }


    @FXML
    public void onToggleLabels(ActionEvent event) {
        if (clusters.isEmpty()) return;
        boolean labels = showLabelsMenuItem.isSelected();
        annotatedImage = LeafRenderer.drawClustersOnImage(originalImage, clusters, labels);
        originalView.setImage(annotatedImage);
    }


    @FXML
    public void onColorAllDisjointSets(ActionEvent event) {
        if (processor == null || clusters.isEmpty()) { status("Detect leaves first."); return; }
        bwView.setImage(LeafRenderer.drawRandomClusterColors(bwImage, clusters));
        status("Disjoint sets coloured randomly in B/W view.");
    }

    @FXML
    public void onRevertBwView(ActionEvent event) {
        if (bwImage != null) {
            bwView.setImage(bwImage);
            status("B/W view restored.");
        }
    }


    private void handleOriginalViewClick(MouseEvent e) {
        if (originalImage == null) return;

        int ix = imageX(e, originalView, originalImage);
        int iy = imageY(e, originalView, originalImage);


        if (samplingMode) {
            Color picked = originalImage.getPixelReader().getColor(ix, iy);
            targetColors.add(picked);
            refreshColorList();
            samplingMode = false;
            status("Colour added: " + toHex(picked) + " — click '+ Sample' again to pick another.");
            return;
        }


        if (e.getButton() == MouseButton.SECONDARY) {
            LeafCluster hit = findClusterAt(ix, iy);
            if (hit != null) {
                tspStartCluster = hit;
                status("TSP start set to cluster #" + hit.getRank()
                        + "  (size=" + hit.getSize() + " px)");
            }
            return;
        }


        if (processor == null || clusters.isEmpty()) return;

        LeafCluster hit = findClusterAt(ix, iy);
        if (hit == null) {
            if (annotatedImage != null) originalView.setImage(annotatedImage);
            if (bwImage != null) bwView.setImage(bwImage);
            selectedInfoLabel.setText("Selected: —");
            return;
        }

        boolean labels = showLabelsMenuItem == null || showLabelsMenuItem.isSelected();
        originalView.setImage(LeafRenderer.highlightSingleCluster(originalImage, clusters, hit, labels));

        if (bwImage != null)
            bwView.setImage(LeafRenderer.highlightClusterPixelsOnBW(bwImage, hit));

        selectedInfoLabel.setText("Selected: #" + hit.getRank()
                + "  |  " + hit.getSize() + " px"
                + "  |  W=" + hit.getWidth() + " H=" + hit.getHeight());
        status("Cluster #" + hit.getRank()
                + "  |  Size: " + hit.getSize() + " px"
                + "  |  [" + hit.getMinX() + "," + hit.getMinY()
                + "] → [" + hit.getMaxX() + "," + hit.getMaxY() + "]");
    }

    private void handleMouseHover(MouseEvent e) {
        if (processor == null || clusters.isEmpty() || originalImage == null) return;
        int ix = imageX(e, originalView, originalImage);
        int iy = imageY(e, originalView, originalImage);
        LeafCluster hit = findClusterAt(ix, iy);
        if (hit != null) {
            status("Cluster #" + hit.getRank()
                    + "  |  " + hit.getSize() + " px"
                    + "  |  W=" + hit.getWidth() + " H=" + hit.getHeight());
        }
    }


    private void handleBwViewClick(MouseEvent e) {
        if (processor == null || clusters.isEmpty() || bwImage == null) return;

        int ix = imageX(e, bwView, bwImage);
        int iy = imageY(e, bwView, bwImage);

        LeafCluster hit = findClusterAt(ix, iy);
        if (hit == null) return;

        bwView.setImage(LeafRenderer.highlightClusterPixelsOnBW(bwImage, hit));

        boolean labels = showLabelsMenuItem == null || showLabelsMenuItem.isSelected();
        originalView.setImage(LeafRenderer.highlightSingleCluster(originalImage, clusters, hit, labels));

        selectedInfoLabel.setText("Selected: #" + hit.getRank()
                + "  |  " + hit.getSize() + " px"
                + "  |  W=" + hit.getWidth() + " H=" + hit.getHeight());
        status("Cluster #" + hit.getRank() + "  |  " + hit.getSize() + " px");
    }


    @FXML
    public void onShowTspPath(ActionEvent event) {
        if (clusters == null || clusters.isEmpty()) { status("Detect leaves first."); return; }

        LeafCluster start = tspStartCluster;
        if (start == null) {
            start = clusters.stream().filter(c -> c.getRank() == 1).findFirst()
                    .orElse(clusters.get(0));
        }

        List<LeafCluster> path = LeafRenderer.computeNearestNeighbourPath(clusters, start);
        animatePath(path);
    }

    private void animatePath(List<LeafCluster> path) {
        if (path.size() < 2) return;

        int totalMs   = 5000;
        int stepDelay = Math.max(50, totalMs / (path.size() - 1));
        boolean labels = showLabelsMenuItem == null || showLabelsMenuItem.isSelected();


        Canvas canvas = new Canvas(originalImage.getWidth(), originalImage.getHeight());
        GraphicsContext gc = canvas.getGraphicsContext2D();


        gc.drawImage(originalImage, 0, 0);
        LeafRenderer.drawBoxesOnGC(gc, clusters, labels);

        Timeline timeline = new Timeline();

        for (int i = 0; i < path.size() - 1; i++) {
            final LeafCluster a = path.get(i);
            final LeafCluster b = path.get(i + 1);

            timeline.getKeyFrames().add(new KeyFrame(
                    Duration.millis((long) i * stepDelay),
                    e -> {

                        LeafRenderer.drawSegmentOnGC(gc, a, b);


                        WritableImage snap = new WritableImage(
                                (int) canvas.getWidth(), (int) canvas.getHeight());
                        canvas.snapshot(new SnapshotParameters(), snap);
                        originalView.setImage(snap);
                    }));
        }

        timeline.getKeyFrames().add(new KeyFrame(
                Duration.millis((long)(path.size() - 1) * stepDelay + 200),
                e -> status("TSP path complete — " + path.size() + " clusters visited.")));

        timeline.play();
        status("Animating TSP path (" + (path.size() - 1) + " steps)...");
    }


    private void resetState() {
        bwImage        = null;
        annotatedImage = null;
        processor      = null;
        tspStartCluster = null;
        clusters.clear();
        bwView.setImage(null);
        resetStatsLabels();
    }

    private void resetStatsLabels() {
        clusterCountLabel.setText("Clusters detected: —");
        selectedInfoLabel.setText("Selected: —");
    }

    private void updateClusterCountLabel() {
        clusterCountLabel.setText("Clusters detected: " + clusters.size());
    }

    @FXML
    public void onReset(ActionEvent event) {
        originalView.setImage(null);
        resetState();
        imageSizeLabel.setText("Image size: —");
        status("Reset. Load a new image.");
    }

    @FXML
    public void onExit(ActionEvent event) {
        originalView.getScene().getWindow().hide();
    }



    private LeafCluster findClusterAt(int x, int y) {
        for (LeafCluster c : clusters) {
            if (x >= c.getMinX() && x <= c.getMaxX()
                    && y >= c.getMinY() && y <= c.getMaxY()) {
                return c;
            }
        }
        return null;
    }

    private int imageX(MouseEvent e, ImageView view, Image img) {
        double scale = img.getWidth() / view.getBoundsInLocal().getWidth();
        return (int) Math.min(img.getWidth()  - 1, Math.max(0, e.getX() * scale));
    }

    private int imageY(MouseEvent e, ImageView view, Image img) {
        double scale = img.getHeight() / view.getBoundsInLocal().getHeight();
        return (int) Math.min(img.getHeight() - 1, Math.max(0, e.getY() * scale));
    }

    private int parse(String s, int def) {
        try { return Integer.parseInt(s.trim()); }
        catch (Exception e) { return def; }
    }

    private void status(String msg) {
        if (statusLabel != null) statusLabel.setText(msg);
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}