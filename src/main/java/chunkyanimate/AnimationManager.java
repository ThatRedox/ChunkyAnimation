package chunkyanimate;

import javafx.application.Platform;
import javafx.scene.control.Label;
import se.llbit.chunky.main.Chunky;
import se.llbit.chunky.renderer.scene.Scene;
import se.llbit.json.JsonParser;
import se.llbit.log.Log;
import se.llbit.util.TaskTracker;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;

public class AnimationManager {

    private Chunky chunky = null;
    private boolean animating = false;
    private int frameCount = 0;
    private int totalFrames = 0;

    private Label progressLabel = null;

    private final ArrayDeque<AnimationFrame> animationFrames = new ArrayDeque<>();

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

            AnimationFrame frame = animationFrames.pollFirst();
            if (frame != null) {
                frame.apply(scene);
                scene.startRender();
                return;
            }
        }

        this.animating = false;
    }

    public void sunCycle() {
        animating = false;
        frameCount = 0;
        animationFrames.clear();

        Scene scene = chunky.getSceneManager().getScene();

        double azimuth = 0.8398896196381793;
        AnimationFrame frame = new AnimationFrame(scene);
        frame.sunAzimuth = azimuth;
        for (double i = -10; i < 90; i++) {
            String frameJson = String.format("{\"sun\": {\"azimuth\": %f, \"altitude\": %f}}",
                    azimuth, Math.toRadians(i));
            System.out.println(frameJson);

            ByteArrayInputStream inputStream = new ByteArrayInputStream(frameJson.getBytes());
            try (JsonParser parser = new JsonParser(inputStream)) {
                frame = new AnimationFrame(parser.parse().asObject(), frame);
            } catch (IOException | JsonParser.SyntaxError e) {
                System.out.println("^ Error");
            }
            animationFrames.add(frame);
        }

        for (double i = 90; i < 190; i++) {
            String frameJson = String.format("{\"sun\": {\"azimuth\": %f, \"altitude\": %f}}",
                    azimuth + Math.PI, Math.toRadians(i));
            System.out.println(frameJson);

            ByteArrayInputStream inputStream = new ByteArrayInputStream(frameJson.getBytes());
            try (JsonParser parser = new JsonParser(inputStream)) {
                frame = new AnimationFrame(parser.parse().asObject(), frame);
            } catch (IOException | JsonParser.SyntaxError e) {
                System.out.println("^ Error");
            }
            animationFrames.add(frame);
        }
    }

    public void setProgressLabel(Label label) {
        this.progressLabel = label;
    }
}
