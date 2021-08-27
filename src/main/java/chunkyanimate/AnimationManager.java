package chunkyanimate;

import javafx.application.Platform;
import javafx.scene.control.Label;
import se.llbit.chunky.main.Chunky;
import se.llbit.chunky.renderer.scene.Scene;
import se.llbit.json.JsonParser;
import se.llbit.log.Log;
import se.llbit.util.TaskTracker;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class AnimationManager {

    private Chunky chunky = null;
    private boolean animating = false;
    private int frameCount = 0;
    private final ArrayList<AnimationFrame> animationFrames = new ArrayList<>();

    private Label progressLabel = null;

    public void setChunky(Chunky chunky) {
        this.chunky = chunky;
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
                frameCount++;
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

    public void setProgressLabel(Label label) {
        this.progressLabel = label;
    }
}
