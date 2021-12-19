package chunkyanimate;

import it.unimi.dsi.fastutil.doubles.Double2ObjectMap;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import se.llbit.chunky.renderer.scene.Scene;
import se.llbit.chunky.ui.DoubleTextField;
import se.llbit.chunky.ui.render.RenderControlsTab;
import se.llbit.log.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class KeyFrameTab implements RenderControlsTab {
    private final AnimationManager manager;
    private final VBox box;
    private final TableView<Double2ObjectMap.Entry<AnimationKeyFrame>> keyframeTable;

    private final DoubleTextField[] keyframeFields =
            new DoubleTextField[AnimationKeyFrame.interpolatableFields().length];

    public KeyFrameTab(AnimationManager manager) {
        this.manager = manager;
        box = new VBox(10.0);
        box.setPadding(new Insets(10.0, 0, 0, 10.0));

        // Keyframe table
        {
            keyframeTable = new TableView<>();
            TableColumn<Double2ObjectMap.Entry<AnimationKeyFrame>, String> nameCol = new TableColumn<>("Name");
            nameCol.setCellValueFactory(frame -> new ReadOnlyStringWrapper(
                    frame.getValue().getValue().keyframeName
            ));
            keyframeTable.getColumns().add(nameCol);
            TableColumn<Double2ObjectMap.Entry<AnimationKeyFrame>, Number> timeCol = new TableColumn<>("Time");
            timeCol.setCellValueFactory(frame -> new ReadOnlyDoubleWrapper(
                    frame.getValue().getDoubleKey()
            ));
            keyframeTable.getColumns().add(timeCol);
            keyframeTable.getSelectionModel().selectedItemProperty().addListener(
                    (observable, oldValue, newValue) -> this.setKeyframeFields(newValue.getValue()));
            box.getChildren().add(keyframeTable);

            HBox saveLoadBox = centeredHBox();
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON File", "*.json"));
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));

            Button saveKeyframes = new Button("Save Keyframes");
            saveKeyframes.setOnAction(e -> {
                for (TextField field : this.keyframeFields) {
                    try {
                        field.getOnAction().handle(null);
                    } catch (NullPointerException ex) {
                        // Ignored
                    }
                }

                File saveFile = fileChooser.showSaveDialog(box.getScene().getWindow());
                if (saveFile == null) return;
                try {
                    manager.saveKeyframes(saveFile);
                } catch (FileNotFoundException ex) {
                    Log.error(ex);
                }
            });
            saveLoadBox.getChildren().add(saveKeyframes);

            Button loadKeyframes = new Button("Load Keyframes");
            loadKeyframes.setOnAction(e -> {
                File loadFile = fileChooser.showOpenDialog(box.getScene().getWindow());
                if (loadFile == null) return;
                try {
                    manager.loadKeyframes(loadFile);
                } catch (IOException ex) {
                    Log.error(ex);
                }
                this.updateManager();
            });
            saveLoadBox.getChildren().add(loadKeyframes);

            box.getChildren().add(saveLoadBox);
        }

        // Add keyframe
        {
            box.getChildren().add(new Separator());

            HBox nameBox = new HBox(10.0);
            nameBox.setAlignment(Pos.CENTER_LEFT);
            nameBox.getChildren().add(new Label("Name:"));
            TextField keyframeNameField = new TextField();
            keyframeNameField.setText("Keyframe");
            nameBox.getChildren().add(keyframeNameField);
            box.getChildren().add(nameBox);

            HBox timeBox = new HBox(10.0);
            timeBox.setAlignment(Pos.CENTER_LEFT);
            timeBox.getChildren().add(new Label("Time:"));
            DoubleTextField keyframeTimeField = new DoubleTextField();
            keyframeTimeField.valueProperty().setValue(0);
            timeBox.getChildren().add(keyframeTimeField);
            box.getChildren().add(timeBox);

            Button addKeyframeButton = new Button("Add Keyframe");
            addKeyframeButton.setOnAction(e -> {
                manager.animationKeyFrames.put(
                        keyframeTimeField.valueProperty().doubleValue(),
                        new AnimationKeyFrame(keyframeNameField.getText())
                );
                Platform.runLater(this::updateManager);
            });
            box.getChildren().add(addKeyframeButton);
        }

        // Keyframe editor
        {
            box.getChildren().add(new Separator());
            String[] interpFields = AnimationKeyFrame.interpolatableFields();
            for (int i = 0; i < interpFields.length; i++) {
                String field = interpFields[i];
                DoubleTextField textField = new DoubleTextField();
                textField.setText("");
                keyframeFields[i] = textField;
                box.getChildren().add(centeredHBox(
                        new Label(field + ": "),
                        textField
                ));
            }
        }

        updateManager();
    }

    private static HBox centeredHBox(Node... nodes) {
        HBox out = new HBox(nodes);
        out.setAlignment(Pos.CENTER_LEFT);
        return out;
    }

    public void updateManager() {
        keyframeTable.getItems().clear();
        keyframeTable.getItems().addAll(manager.animationKeyFrames.double2ObjectEntrySet());
    }

    private void setKeyframeFields(AnimationKeyFrame keyFrame) {
        String[] interpFields = AnimationKeyFrame.interpolatableFields();
        for (int i = 0; i < interpFields.length; i++) {
            String fieldName = interpFields[i];
            TextField field = keyframeFields[i];
            try {
                field.getOnAction().handle(null);
            } catch (NullPointerException e) {
                // Ignored
            }
            if (keyFrame.interpFields.containsKey(fieldName)) {
                field.setText(Double.toString(keyFrame.interpFields.get(fieldName)));
            } else {
                field.setText("");
            }
            field.setOnAction(e -> {
                try {
                    double value = Double.parseDouble(field.getText());
                    keyFrame.interpFields.put(fieldName, value);
                } catch (NullPointerException | NumberFormatException ignored) {
                    keyFrame.interpFields.remove(fieldName);
                }
            });
        }
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
