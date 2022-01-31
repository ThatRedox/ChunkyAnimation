package dev.thatredox.chunky.animate.plugin;

import dev.thatredox.chunky.animate.animation.AnimationFrame;
import dev.thatredox.chunky.animate.animation.AnimationKeyFrame;
import dev.thatredox.chunky.animate.animation.AnimationUtils;
import dev.thatredox.chunky.animate.reflection.DoubleField;
import dev.thatredox.chunky.animate.reflection.DoubleJsonField;
import dev.thatredox.chunky.animate.reflection.DoubleSceneField;
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
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import se.llbit.chunky.renderer.scene.Scene;
import se.llbit.chunky.ui.DoubleAdjuster;
import se.llbit.chunky.ui.DoubleTextField;
import se.llbit.chunky.ui.render.RenderControlsTab;
import se.llbit.log.Log;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KeyFrameTab implements RenderControlsTab {
    private final AnimationManager manager;
    private final VBox box;
    private final TableView<Double2ObjectMap.Entry<AnimationKeyFrame>> keyframeTable;

    private final DoubleTextField[] keyframeFields =
            new DoubleTextField[AnimationKeyFrame.interpolatableFields().length];

    private final TextField keyframeNameField = new TextField();
    private final DoubleTextField keyframeTimeField = new DoubleTextField();

    private final ExecutorService calculationExecutor = Executors.newSingleThreadExecutor();
    private Map<String, PolynomialSplineFunction> interpCache = null;
    private double interpMaxTime;
    private long previewStart = -1;

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
                        if (newValue != null) this.setKeyframeFields(observable.getValue());
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
            keyframeNameField.setText("Keyframe");
            nameBox.getChildren().add(keyframeNameField);
            box.getChildren().add(nameBox);

            HBox timeBox = new HBox(10.0);
            timeBox.setAlignment(Pos.CENTER_LEFT);
            timeBox.getChildren().add(new Label("Time:"));
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
            Button removeKeyframeButton = new Button("Remove Keyframe");
            removeKeyframeButton.setOnAction(e -> {
                manager.animationKeyFrames.remove(keyframeTimeField.valueProperty().doubleValue());
                this.updateManager();
            });

            box.getChildren().add(centeredHBox(addKeyframeButton, removeKeyframeButton));
        }

        // Keyframe time slider
        {
            box.getChildren().add(new Separator());

            DoubleAdjuster timeAdjuster = new DoubleAdjuster();
            timeAdjuster.setName("Time");
            timeAdjuster.setRange(0, 1);
            timeAdjuster.clampBoth();
            timeAdjuster.setTooltip("Preview an animation time.");

            timeAdjuster.setOnMouseEntered(event -> {
                if (manager.animationKeyFrames.size() > 0) {
                    timeAdjuster.setRange(0, manager.animationKeyFrames.lastDoubleKey());
                    calculationExecutor.submit(() -> {
                        this.interpMaxTime = manager.animationKeyFrames.lastDoubleKey();
                        this.interpCache = AnimationUtils.interpolateKeyframes(manager.animationKeyFrames, (progress, total) -> true);
                    });
                }
            });
            timeAdjuster.valueProperty().addListener((observable, oldValue, newValue) -> calculationExecutor.submit(() -> {
                if (this.interpCache != null && scene != null) {
                    AnimationFrame frame = new AnimationFrame(scene);
                    frame = AnimationUtils.applyInterpolation(interpCache,
                            (Double) newValue, this.interpMaxTime, frame);
                    frame.apply(scene);
                    scene.refresh();
                }
            }));
            box.getChildren().add(timeAdjuster);

            Button previewPlayPause = new Button("Play Preview");
            previewPlayPause.setOnAction(event -> {
                if (this.previewStart == -1) {
                    Platform.runLater(() -> previewPlayPause.setText("Pause Preview"));
                    this.previewStart = System.currentTimeMillis();
                    calculationExecutor.submit(() -> {
                        double currentTime;
                        do {
                            long previewStart = this.previewStart;
                            if (previewStart == -1) {
                                break;
                            }

                            currentTime = (System.currentTimeMillis() - previewStart) / 1000.0;
                            if (currentTime >= this.interpMaxTime) {
                                currentTime = this.interpMaxTime;
                            }

                            if (this.interpCache == null || scene == null) {
                                break;
                            }

                            double finalCurrentTime = currentTime;
                            Platform.runLater(() -> timeAdjuster.set(finalCurrentTime));

                            AnimationFrame frame = new AnimationFrame(scene);
                            frame = AnimationUtils.applyInterpolation(interpCache,
                                    currentTime, this.interpMaxTime, frame);

                            try {
                                synchronized (manager.renderUpdateEvent) {
                                    frame.apply(scene);
                                    scene.refresh();
                                    manager.renderUpdateEvent.wait(1000);
                                }
                            } catch (InterruptedException e) {
                                break;
                            }
                        } while (currentTime < this.interpMaxTime);

                        Platform.runLater(() -> previewPlayPause.setText("Play Preview"));
                    });
                } else {
                    Platform.runLater(() -> previewPlayPause.setText("Play Preview"));
                    this.previewStart = -1;
                }
            });
            box.getChildren().add(previewPlayPause);
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

    private void setKeyframeFields(Double2ObjectMap.Entry<AnimationKeyFrame> entry) {
        AnimationKeyFrame keyFrame = entry.getValue();

        this.keyframeNameField.setText(keyFrame.keyframeName);
        this.keyframeNameField.setOnAction(event -> {
            keyFrame.keyframeName = this.keyframeNameField.getText();
            this.updateManager();
        });

        this.keyframeTimeField.valueProperty().setValue(entry.getDoubleKey());
        this.keyframeTimeField.setOnAction(event -> {
            manager.animationKeyFrames.remove(entry.getDoubleKey());
            manager.animationKeyFrames.put(this.keyframeTimeField.valueProperty().doubleValue(), keyFrame);
            this.updateManager();
        });

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
