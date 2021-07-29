package chunkyanimate;

import javafx.application.Platform;
import javafx.scene.control.Label;
import se.llbit.chunky.main.Chunky;
import se.llbit.chunky.renderer.scene.Scene;
import se.llbit.log.Log;
import se.llbit.math.Vector3;
import se.llbit.util.TaskTracker;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

public class AnimationManager {
    @FunctionalInterface
    public interface AnimationTask {
        boolean apply(Scene scene);
    }

    private Chunky chunky = null;
    private boolean animating = false;
    private int frameCount = 0;
    private int totalFrames = 0;

    private Label progressLabel = null;

    private final LinkedList<AnimationTask> animationFrames = new LinkedList<>();

    public void setChunky(Chunky chunky) {
        this.chunky = chunky;
    }

    public void saveFrame(int count) {
        File baseDir = chunky.getRenderController().getContext().getSceneDirectory();
        String path = baseDir.getPath();
        path += String.format("%sanimate%sframe%05d.png", File.separator, File.separator, count);
        File saveFile = new File(path);
        saveFile.getParentFile().mkdirs();
        try {
            saveFile.createNewFile();
        } catch (IOException e) {
            Log.error(e);
            return;
        }

        chunky.getSceneManager().getScene().saveFrame(saveFile, TaskTracker.NONE,
                chunky.getRenderController().getContext().numRenderThreads());
    }

    public void frameComplete() {
        if (animating) {
            chunky.getSceneManager().getScene().pauseRender();
            saveFrame(this.frameCount);
            this.frameCount++;

            if (progressLabel != null) {
                Platform.runLater(() -> progressLabel.setText(String.format("Frame %d / %d", frameCount, totalFrames)));
            }
            runUntilRender();
        }
    }

    public void startAnimation() {
        this.animating = true;
        this.totalFrames = animationFrames.size();
        Platform.runLater(() -> progressLabel.setText(String.format("Frame 0 / %d", this.totalFrames)));
        runUntilRender();
    }

    public void stopAnimation() {
        this.animating = false;
        this.totalFrames = 0;
    }

    public void runUntilRender() {
        Scene scene = chunky.getSceneManager().getScene();
        synchronized (scene) {
            scene.haltRender();
            scene.forceReset();
            AnimationTask task;
            while ((task = animationFrames.pollFirst()) != null) {
                if (task.apply(scene)) {
                    return;
                }
            }
        }

        this.animating = false;
    }

    public void sunCycle() {
        animating = false;
        frameCount = 0;
        animationFrames.clear();

        for (double i = -10; i < 90; i++) {
            double finalI = i;
            animationFrames.add(scene -> {
                scene.sun().setAzimuth(0.8398896196381793);
                scene.sun().setAltitude(Math.toRadians(finalI));
                scene.startRender();
                return true;
            });
        }

        for (double i = 90; i < 190; i++) {
            double finalI = i;
            animationFrames.add(scene -> {
                scene.sun().setAzimuth(0.8398896196381793 + Math.PI);
                scene.sun().setAltitude(Math.toRadians(180 - finalI));
                scene.refresh();
                scene.startRender();
                return true;
            });
        }
    }

    public void cameraDescend() {
        animating = false;
        frameCount = 0;
        animationFrames.clear();

        for (double i = 1000; i > 441; i -= (1000 - 441) / 100.0) {
            double finalI = i;
            animationFrames.add(scene -> {
                scene.camera().setPosition(new Vector3(2049.5, finalI, 3671.5));
                scene.startRender();
                return true;
            });
        }
    }

    public void setProgressLabel(Label label) {
        this.progressLabel = label;
    }
}
