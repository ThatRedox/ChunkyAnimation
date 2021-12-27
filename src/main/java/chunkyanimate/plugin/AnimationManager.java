package chunkyanimate.plugin;

import chunkyanimate.animation.AnimationFrame;
import chunkyanimate.animation.AnimationKeyFrame;
import chunkyanimate.animation.AnimationUtils;
import it.unimi.dsi.fastutil.doubles.Double2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.doubles.Double2ObjectSortedMap;
import javafx.application.Platform;
import javafx.scene.control.Label;
import se.llbit.chunky.main.Chunky;
import se.llbit.chunky.renderer.scene.Scene;
import se.llbit.log.Log;
import se.llbit.util.TaskTracker;

import java.io.*;
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

    public void saveKeyframes(File out) throws IOException {
        AnimationUtils.saveKeyframes(out, animationKeyFrames);
    }

    public void loadKeyframes(File in) throws IOException {
        AnimationUtils.loadKeyframes(in, animationKeyFrames);
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
        animating = false;
        frameCount = 0;
        animationFrames.clear();
        updateLabel();

        AnimationUtils.loadFramesFromFolder(
                folder,
                animationFrames,
                chunky.getSceneManager().getScene()
        );
        updateLabel();
    }

    public void fromKeyFrames(double framerate) {
        animating = false;
        frameCount = 0;
        animationFrames.clear();
        updateLabel();

        AnimationUtils.loadFramesFromKeyframes(
                animationKeyFrames,
                framerate,
                animationFrames,
                chunky.getSceneManager().getScene()
        );
        updateLabel();
    }

    public void setProgressLabel(Label label) {
        this.progressLabel = label;
    }
}
