package chunkyanimate.plugin;

import chunkyanimate.animation.AnimationFrame;
import chunkyanimate.animation.AnimationKeyFrame;
import chunkyanimate.reflection.DoubleField;
import chunkyanimate.reflection.DoubleJsonField;
import chunkyanimate.reflection.DoubleSceneField;
import it.unimi.dsi.fastutil.doubles.*;
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
import java.lang.reflect.Field;

public class KeyFrameTab implements RenderControlsTab {
    private final AnimationManager manager;
    private final VBox box;
    private final TableView<Double2ObjectMap.Entry<AnimationKeyFrame>> keyframeTable;

    private final DoubleTextField[] keyframeFields =
            new DoubleTextField[AnimationKeyFrame.interpolatableFields().length];

    private Scene scene = null;

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
                    (observable, oldValue, newValue) -> {
                        if (newValue != null) this.setKeyframeFields(newValue.getValue());
                    });
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
                } catch (IOException ex) {
                    Log.error("Failed to save keyframes", ex);
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
            Field[] interpFields = AnimationKeyFrame.interpolatableFields();
            for (int i = 0; i < interpFields.length; i++) {
                Field field = interpFields[i];
                DoubleSceneField sceneField = field.getAnnotation(DoubleSceneField.class);
                DoubleJsonField jsonField = field.getAnnotation(DoubleJsonField.class);
                DoubleField fieldValue = field.getAnnotation(DoubleField.class);
                String fieldName = fieldValue == null ? field.getName() : fieldValue.value();

                DoubleTextField textField = new DoubleTextField();
                textField.setText("");
                Button currentValueButton = new Button("From Scene");
                currentValueButton.setOnAction(e -> {
                    if (scene != null) {
                        double value = Double.NaN;
                        if (sceneField != null) {
                            value = AnimationFrame.resolveSceneDoubleField(scene, sceneField.value());
                        } else if (jsonField != null) {
                            value = AnimationFrame.resolveJsonField(scene.toJson(), jsonField.value()).doubleValue(Double.NaN);
                        }
                        if (!Double.isNaN(value)) {
                            if (fieldValue != null && fieldValue.inRadians()) {
                                value = Math.toDegrees(value);
                            }
                            textField.setText(Double.toString(value));
                        }
                    }
                });
                keyframeFields[i] = textField;
                box.getChildren().add(centeredHBox(
                        new Label(fieldName + ": "),
                        textField,
                        currentValueButton
                ));
            }
        }

        updateManager();
    }

    private static HBox centeredHBox(Node... nodes) {
        HBox out = new HBox(nodes);
        out.setSpacing(10.0);
        out.setAlignment(Pos.CENTER_LEFT);
        return out;
    }

    public void updateManager() {
        Double2ObjectMap.Entry<AnimationKeyFrame> entry = keyframeTable.getSelectionModel().getSelectedItem();
        double selectedTime = entry != null ? entry.getDoubleKey() : Double.NaN;

        keyframeTable.getItems().clear();
        keyframeTable.getItems().addAll(manager.animationKeyFrames.double2ObjectEntrySet());
        keyframeTable.getItems().stream()
                .filter(e -> e.getDoubleKey() == selectedTime)
                .findFirst()
                .ifPresent(e -> keyframeTable.getSelectionModel().select(e));
    }

    private void setKeyframeFields(AnimationKeyFrame keyFrame) {
        Field[] interpFields = AnimationKeyFrame.interpolatableFields();
        for (int i = 0; i < interpFields.length; i++) {
            String fieldName = interpFields[i].getName();
            boolean inRadians = interpFields[i].isAnnotationPresent(DoubleField.class) &&
                    interpFields[i].getAnnotation(DoubleField.class).inRadians();

            TextField field = keyframeFields[i];
            try {
                field.getOnAction().handle(null);
            } catch (NullPointerException e) {
                // Ignored
            }
            if (keyFrame.interpFields.containsKey(fieldName)) {
                double value = keyFrame.interpFields.get(fieldName);
                if (inRadians) {
                    value = Math.toDegrees(value);
                }
                field.setText(Double.toString(value));
            } else {
                field.setText("");
            }
            field.setOnAction(e -> {
                try {
                    double value = Double.parseDouble(field.getText());
                    if (inRadians) {
                        value = Math.toRadians(value);
                    }
                    keyFrame.interpFields.put(fieldName, value);
                } catch (NullPointerException | NumberFormatException ignored) {
                    keyFrame.interpFields.remove(fieldName);
                }
            });
        }
    }

    @Override
    public void update(Scene scene) {
        this.scene = scene;
    }

    @Override
    public String getTabTitle() {
        return "Keyframe Editor";
    }

    @Override
    public Node getTabContent() {
        return box;
    }
}
