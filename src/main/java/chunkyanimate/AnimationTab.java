package chunkyanimate;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import se.llbit.chunky.renderer.scene.Scene;
import se.llbit.chunky.ui.render.RenderControlsTab;

import java.io.File;

public class AnimationTab implements RenderControlsTab {
    private final VBox box;

    public AnimationTab(AnimationManager manager) {
        box = new VBox(10.0);
        box.setPadding(new Insets(10.0, 0, 0, 10.0));

        DirectoryChooser chooser = new DirectoryChooser();
        Button directoryButton = new Button("Choose a Folder");
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
