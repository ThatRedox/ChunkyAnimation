package chunkyanimate.plugin;

import chunkyanimate.util.ObservableValue;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import se.llbit.chunky.renderer.scene.Scene;
import se.llbit.chunky.ui.DoubleTextField;
import se.llbit.chunky.ui.render.RenderControlsTab;

import java.io.File;

public class AnimationTab implements RenderControlsTab {
    private final VBox box;
    private long renderStart = 0;

    public AnimationTab(AnimationManager manager) {
        box = new VBox(10.0);
        box.setPadding(new Insets(10.0, 0, 10.0, 10.0));

        {
            HBox fromKeyFramesBox = new HBox(10);
            fromKeyFramesBox.alignmentProperty().set(Pos.CENTER_LEFT);
            fromKeyFramesBox.getChildren().add(new Label("Framerate:"));

            DoubleTextField framerateField = new DoubleTextField();
            framerateField.valueProperty().setValue(30);
            fromKeyFramesBox.getChildren().add(framerateField);

            Button keyFramesButton = new Button("From Keyframes");
            keyFramesButton.setOnAction(e -> manager.fromKeyFrames(framerateField.valueProperty().doubleValue()));
            fromKeyFramesBox.getChildren().add(keyFramesButton);

            box.getChildren().add(fromKeyFramesBox);
        }

        DirectoryChooser chooser = new DirectoryChooser();
        Button directoryButton = new Button("Load Frames From Folder");
        directoryButton.setOnAction(e -> {
            File dir = chooser.showDialog(box.getScene().getWindow());
            manager.fromFolder(dir);
        });
        box.getChildren().add(directoryButton);

        Button startAnimationButton = new Button("Start Animation");
        startAnimationButton.setOnAction(e -> manager.startAnimation());
        box.getChildren().add(startAnimationButton);

        Button stopAnimationButton = new Button("Stop Animation");
        stopAnimationButton.setOnAction(e -> manager.stopAnimation());
        box.getChildren().add(stopAnimationButton);

        Label progressLabel = new Label("Frame 0 / 0");
        box.getChildren().add(progressLabel);

        Label etaLabel = new Label("ETA: N/A");
        box.getChildren().add(etaLabel);

        ObservableValue.ChangeListener updateListener = v -> {
            int currentFrame = manager.currentFrame.getValue();
            int totalFrames = manager.totalFrames.getValue();
            long currentTime = System.currentTimeMillis();

            if (currentFrame == 0) {
                renderStart = System.currentTimeMillis();
            }

            Platform.runLater(() -> {
                progressLabel.setText(String.format("Frame %d / %d", currentFrame, totalFrames));
                if (manager.isAnimating() && manager.currentFrame.getValue() > 1) {
                    long etaSeconds = ((totalFrames - currentFrame) * (currentTime - renderStart)) / (currentFrame * 1000L);
                    int etaMinutes = (int) ((etaSeconds / 60) % 60);
                    int etaHours = (int) (etaSeconds / 3600);
                    etaLabel.setText(String.format("ETA: %02d:%02d:%02d", etaHours, etaMinutes, etaSeconds % 60));
                } else {
                    etaLabel.setText("ETA: N/A");
                }
            });
        };
        manager.currentFrame.addListener(updateListener);
        manager.totalFrames.addListener(updateListener);
    }

    @Override
    public void update(Scene scene) {

    }

    @Override
    public String getTabTitle() {
        return "Animation Controls";
    }

    @Override
    public Node getTabContent() {
        return box;
    }
}
