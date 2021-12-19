package chunkyanimate;

import it.unimi.dsi.fastutil.doubles.Double2ObjectMap;
import it.unimi.dsi.fastutil.doubles.Double2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.doubles.Double2ObjectSortedMap;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import javafx.application.Platform;
import javafx.scene.control.Label;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import se.llbit.chunky.main.Chunky;
import se.llbit.chunky.renderer.scene.Scene;
import se.llbit.json.JsonObject;
import se.llbit.json.JsonParser;
import se.llbit.json.JsonValue;
import se.llbit.json.PrettyPrinter;
import se.llbit.log.Log;
import se.llbit.math.QuickMath;
import se.llbit.util.TaskTracker;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

public class AnimationManager {

    private Chunky chunky = null;
    private boolean animating = false;
    private int frameCount = 0;
    private final ArrayList<AnimationFrame> animationFrames = new ArrayList<>();
    public final Double2ObjectSortedMap<AnimationKeyFrame> animationKeyFrames = new Double2ObjectRBTreeMap<>();

    private Label progressLabel = null;

    public void setChunky(Chunky chunky) {
        this.chunky = chunky;
    }

    public JsonObject keyframesJson() {
        JsonObject out = new JsonObject();
        for (Double2ObjectMap.Entry<AnimationKeyFrame> frame : animationKeyFrames.double2ObjectEntrySet()) {
            out.set(Double.toString(frame.getDoubleKey()), frame.getValue().toJson());
        }
        return out;
    }

    public void saveKeyframes(File out) throws FileNotFoundException {
        try (PrettyPrinter pp = new PrettyPrinter("  ",
                new PrintStream(new BufferedOutputStream(new FileOutputStream(out))))) {
            JsonObject keyframes = this.keyframesJson();
            keyframes.prettyPrint(pp);
        }
    }

    public void loadKeyframesJson(JsonObject obj) {
        for (Map.Entry<String, JsonValue> entry : obj.toMap().entrySet()) {
            try {
                double value = Double.parseDouble(entry.getKey());
                if (entry.getValue().isObject()) {
                    AnimationKeyFrame frame = new AnimationKeyFrame(entry.getValue().asObject());
                    animationKeyFrames.put(value, frame);
                }
            } catch (NullPointerException | NumberFormatException e) {
                // Ignored
            }
        }
    }

    public void loadKeyframes(File in) throws IOException {
        try (JsonParser parser = new JsonParser(new BufferedInputStream(new FileInputStream(in)))) {
            JsonObject obj = parser.parse().object();
            this.loadKeyframesJson(obj);
        } catch (JsonParser.SyntaxError e) {
            Log.error("Syntax error while loading keyframes: ", e);
        }
    }

    public void saveFrame(int count) {
        Scene scene = chunky.getSceneManager().getScene();

        File baseDir = chunky.getRenderController().getContext().getSceneDirectory();
        String path = baseDir.getPath();
        path += String.format("%sanimate%sframe%05d%s",
                File.separator,
                File.separator,
                count,
                scene.getOutputMode().getExtension());

        File saveFile = new File(path);
        File parentDir = saveFile.getParentFile();
        if (!parentDir.exists() && !saveFile.getParentFile().mkdirs()) {
            Log.error("Failed to create output directory: " + saveFile.getParentFile().getPath());
            return;
        }
        try {
            if (!saveFile.createNewFile()) {
                Log.error("File already exists: " + saveFile.getPath());
                return;
            }
        } catch (IOException e) {
            Log.error("Failed to create output file", e);
            return;
        }

        scene.saveFrame(saveFile, TaskTracker.NONE, chunky.getRenderController().getContext().numRenderThreads());
    }

    public void updateLabel() {
        if (progressLabel != null) {
            Platform.runLater(() -> progressLabel.setText(String.format("Frame %d / %d",
                    frameCount, animationFrames.size())));
        }
    }

    public void frameComplete() {
        if (animating) {
            chunky.getSceneManager().getScene().pauseRender();
            saveFrame(this.frameCount);
            this.frameCount++;

            updateLabel();
            runUntilRender();
        }
    }

    public void startAnimation() {
        this.animating = true;
        this.frameCount = 0;
        updateLabel();
        runUntilRender();
    }

    public void stopAnimation() {
        this.animating = false;
    }

    public void runUntilRender() {
        Scene scene = chunky.getSceneManager().getScene();
        synchronized (scene) {
            scene.haltRender();
            scene.forceReset();

            if (frameCount < animationFrames.size()) {
                AnimationFrame frame = animationFrames.get(frameCount);
                if (frame != null) {
                    frame.apply(scene);
                    scene.startRender();
                    return;
                }
            }
        }

        this.animating = false;
    }

    public void fromFolder(File folder) {
        if (folder == null) return;

        animating = false;
        frameCount = 0;
        animationFrames.clear();

        Scene scene = chunky.getSceneManager().getScene();
        AnimationFrame frame = new AnimationFrame(scene);

        File[] files = folder.listFiles();
        if (files == null) return;
        Arrays.sort(files, Comparator.comparing(File::getName));

        for (File file : files) {
            if (file.getName().endsWith(".json")) {
                try (FileInputStream inStream = new FileInputStream(file)){
                    JsonParser parser = new JsonParser(inStream);
                    frame = new AnimationFrame(parser.parse().asObject(), frame);
                    animationFrames.add(frame);
                } catch (JsonParser.SyntaxError | IOException e) {
                    e.printStackTrace();
                }
            }
        }
        updateLabel();
    }

    public void fromKeyFrames(double framerate) {
        if (framerate == 0) return;

        animating = false;
        frameCount = 0;
        animationFrames.clear();

        updateLabel();

        double endTime = animationKeyFrames.lastDoubleKey();
        int numFrames = (int) (endTime * framerate) + 1;
        if (numFrames < 1) return;

        int numKeyFrames = animationKeyFrames.size();
        Map<String, PolynomialSplineFunction> interps = new HashMap<>();
        DoubleArrayList times = new DoubleArrayList(numKeyFrames);
        ArrayList<Double> entries = new ArrayList<>(numKeyFrames);
        Field[] fields = AnimationKeyFrame.interpolatableFields();
        for (Field field : fields) {
            times.clear();
            entries.clear();
            for (Double2ObjectMap.Entry<AnimationKeyFrame> keyFrameEntry : animationKeyFrames.double2ObjectEntrySet()) {
                AnimationKeyFrame keyFrame = keyFrameEntry.getValue();
                if (keyFrame.interpFields.containsKey(field.getName())) {
                    times.add(keyFrameEntry.getDoubleKey());
                    entries.add(keyFrame.interpFields.get(field.getName()));
                }
            }

            if (!times.isEmpty()) {
                if (times.getDouble(0) > 0) {
                    times.add(0, 0);
                    entries.add(0, entries.get(0));
                }
                if (times.getDouble(times.size()-1) != endTime) {
                    times.add(endTime);
                    entries.add(entries.get(entries.size() - 1));
                }
                if (times.size() == 2) {
                    times.add(1, (times.getDouble(0) + times.getDouble(1)) / 2);
                    entries.add(1, (entries.get(0) + entries.get(1)) / 2);
                }

                double[] entriesArray = new double[entries.size()];
                Arrays.setAll(entriesArray, entries::get);
                interps.put(field.getName(), new SplineInterpolator().interpolate(
                        times.toArray(new double[0]),
                        entriesArray));
            }
        }

        animationFrames.ensureCapacity(numFrames);
        AnimationFrame prevFrame = new AnimationFrame(chunky.getSceneManager().getScene());
        for (int i = 0; i < numFrames; i++) {
            double frameTime = QuickMath.clamp(i / framerate, 0, endTime);
            prevFrame = new AnimationFrame(field -> {
                        if (interps.containsKey(field)) {
                            return OptionalDouble.of(interps.get(field).value(frameTime));
                        } else {
                            return OptionalDouble.empty();
                        }
                    }, prevFrame);
            animationFrames.add(prevFrame);
        }

        updateLabel();
    }

    public void setProgressLabel(Label label) {
        this.progressLabel = label;
    }
}
