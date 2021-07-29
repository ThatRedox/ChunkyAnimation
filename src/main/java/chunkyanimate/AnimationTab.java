package chunkyanimate;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import se.llbit.chunky.renderer.scene.Scene;
import se.llbit.chunky.ui.render.RenderControlsTab;

public class AnimationTab implements RenderControlsTab {
    private final VBox box;

    public AnimationTab(AnimationManager manager) {
        box = new VBox(10.0);

        Button testButton = new Button("Sun cycle");
        testButton.setOnAction(e -> manager.sunCycle());
        box.getChildren().add(testButton);

        Button cameraDescendButton = new Button("Camera Descend");
        cameraDescendButton.setOnAction(e -> manager.cameraDescend());
        box.getChildren().add(cameraDescendButton);

        Button startAnimationButton = new Button("Start Animation");
        startAnimationButton.setOnAction(e -> manager.startAnimation());
        box.getChildren().add(startAnimationButton);

        Button stopAnimationButton = new Button("Stop Animation");
        stopAnimationButton.setOnAction(e -> manager.stopAnimation());
        box.getChildren().add(stopAnimationButton);

        Label progressLabel = new Label("Frame 0 / 0");
        manager.setProgressLabel(progressLabel);
        box.getChildren().add(progressLabel);
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
