package chunkyanimate;

import it.unimi.dsi.fastutil.doubles.Double2ObjectMap;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import se.llbit.chunky.renderer.scene.Scene;
import se.llbit.chunky.ui.DoubleTextField;
import se.llbit.chunky.ui.render.RenderControlsTab;

public class KeyFrameTab implements RenderControlsTab {
    private final AnimationManager manager;
    private final VBox box;
    private final TableView<Double2ObjectMap.Entry<AnimationKeyFrame>> keyframeTable;

    public KeyFrameTab(AnimationManager manager) {
        this.manager = manager;
        box = new VBox(10.0);
        box.setPadding(new Insets(10.0, 0, 0, 10.0));

        keyframeTable = new TableView<>();
        TableColumn<Double2ObjectMap.Entry<AnimationKeyFrame>, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(frame -> new ReadOnlyStringWrapper(
                frame.getValue().getValue().keyframeName.orElse("Keyframe")
        ));
        keyframeTable.getColumns().add(nameCol);
        TableColumn<Double2ObjectMap.Entry<AnimationKeyFrame>, Number> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(frame -> new ReadOnlyDoubleWrapper(
                frame.getValue().getDoubleKey()
        ));
        keyframeTable.getColumns().add(timeCol);
        box.getChildren().add(keyframeTable);

        {
            HBox newKeyframeBox = new HBox(10.0);
            newKeyframeBox.getChildren().add(new Label("Time:"));

            DoubleTextField keyframeTimeField = new DoubleTextField();
            keyframeTimeField.valueProperty().setValue(0);
            newKeyframeBox.getChildren().add(keyframeTimeField);

            Button addKeyframeButton = new Button("Add Keyframe");
            addKeyframeButton.setOnAction(e -> {
                manager.animationKeyFrames.put(keyframeTimeField.valueProperty().doubleValue(), new AnimationKeyFrame());
                Platform.runLater(this::updateManager);
            });
            newKeyframeBox.getChildren().add(addKeyframeButton);

            box.getChildren().add(newKeyframeBox);
        }

        Button triggerDebugger = new Button("Trigger Debugger");
        triggerDebugger.setOnAction(e -> {
            Double2ObjectMap.Entry<AnimationKeyFrame> keyFrameEntry = keyframeTable.getSelectionModel().getSelectedItem();
            AnimationManager m = manager;
            System.out.println("Debugger");
        });
        box.getChildren().add(triggerDebugger);

        updateManager();
    }

    public void updateManager() {
        keyframeTable.getItems().clear();
        keyframeTable.getItems().addAll(manager.animationKeyFrames.double2ObjectEntrySet());
    }

    @Override
    public void update(Scene scene) { }

    @Override
    public String getTabTitle() {
        return "Keyframe Editor";
    }

    @Override
    public Node getTabContent() {
        return box;
    }
}
