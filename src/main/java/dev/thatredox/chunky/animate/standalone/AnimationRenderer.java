package dev.thatredox.chunky.animate.standalone;

import dev.thatredox.chunky.animate.animation.AnimationFrame;
import dev.thatredox.chunky.animate.animation.AnimationKeyFrame;
import dev.thatredox.chunky.animate.animation.AnimationUtils;
import it.unimi.dsi.fastutil.doubles.Double2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.doubles.Double2ObjectSortedMap;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import se.llbit.chunky.PersistentSettings;
import se.llbit.chunky.main.Chunky;
import se.llbit.chunky.main.ChunkyOptions;
import se.llbit.chunky.renderer.*;
import se.llbit.chunky.renderer.scene.PreviewRayTracer;
import se.llbit.chunky.renderer.scene.Scene;
import se.llbit.util.TaskTracker;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;

public class AnimationRenderer {
    public static final Options OPTIONS;
    static {
        OPTIONS = new Options();
        OPTIONS.addRequiredOption("r", "render", false, "Required to enter rendering mode.");
        OPTIONS.addRequiredOption("i", "input", true, "Path to the input frames. Can either be a folder of frames or a JSON of keyframes.");
        OPTIONS.addRequiredOption("s", "scene", true, "Scene to render. Must be path or resolvable scene name.");
        OPTIONS.addOption("o", "output", true, "Path to output rendered frames.");
        OPTIONS.addOption("f", "framerate", true, "Framerate to render in if using keyframes.");
        OPTIONS.addOption("t", "threads", true, "Number of threads to render with.");
        OPTIONS.addOption("spp", true, "Override SPP.");
    }

    public static int runRender(String[] args) throws Exception {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(OPTIONS, args, false);

        String inputFramesPath = cmd.getOptionValue("input");
        String scenePath = cmd.getOptionValue("scene");
        String outputPath = cmd.getOptionValue("output", "animation/");
        double framerate = Double.parseDouble(cmd.getOptionValue("framerate", "1.0"));
        int numThreads = Integer.parseInt(cmd.getOptionValue("threads", Integer.toString(PersistentSettings.getNumThreads())));
        int spp = Integer.parseInt(cmd.getOptionValue("spp", "-1"));

        ChunkyOptions chunkyOptions = ChunkyOptions.getDefaults();
        chunkyOptions.renderThreads = numThreads;
        Chunky.loadDefaultTextures();
        Chunky chunky = new Chunky(chunkyOptions);

        Chunky.addRenderer(new PathTracingRenderer("AnimationPreview", "AnimationPreview", "AnimationPreviewRenderer", new PreviewRayTracer()));

        Field chunkyHeadless = chunky.getClass().getDeclaredField("headless");
        chunkyHeadless.setAccessible(true);
        chunkyHeadless.set(chunky, true);

        System.out.println("Loading scene...");

        chunky.getSceneManager().loadScene(scenePath);
        Scene scene = chunky.getSceneManager().getScene();

        ArrayList<AnimationFrame> animationFrames = new ArrayList<>();
        File inputFrames = new File(inputFramesPath);
        if (!inputFrames.exists()) {
            System.err.printf("Input frames path does not exist: %s\n", inputFramesPath);
            return 128;
        }
        if (inputFrames.isDirectory()) {
            AnimationUtils.loadFramesFromFolder(inputFrames, animationFrames, scene);
        } else {
            Double2ObjectSortedMap<AnimationKeyFrame> keyframes = new Double2ObjectRBTreeMap<>();
            AnimationUtils.loadKeyframes(inputFrames, keyframes);
            AnimationUtils.loadFramesFromKeyframes(keyframes, framerate, animationFrames, scene);
        }

        File outputDirectory = new File(outputPath);
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        } else if (!outputDirectory.isDirectory()) {
            System.err.printf("Output must be directory: %s\n", outputPath);
            return 128;
        }

        int numFrames = animationFrames.size();
        long startTime = System.currentTimeMillis();
        TaskTracker taskTracker = new TaskTracker(new ConsoleProgressListener(),
                (tracker, previous, name, size) -> new TaskTracker.Task(tracker, previous, name, size) {
                    @Override
                    public void close() {
                        super.close();
                        long endTime = System.currentTimeMillis();
                        int seconds = (int) ((endTime - startTime) / 1000);
                        System.out.format("\r%s took %dm %ds%n", name, seconds / 60, seconds % 60);
                    }
                });
        for (int i = 0; i < numFrames; i++) {
            String etaString = "N/A";
            if (i > 0) {
                long etaSeconds = (((numFrames - i) * (System.currentTimeMillis() - startTime)) / (i * 1000L));
                etaString = String.format("%d h, %02d min", etaSeconds / 3600, (etaSeconds / 60) % 60);
            }
            System.out.printf("\nRendering frame %d out of %d. [ETA=%s]\n",
                    i + 1, numFrames, etaString);
            try (TaskTracker.Task renderTask = taskTracker.task("Rendering")) {
                DefaultRenderManager renderer = new DefaultRenderManager(chunky.getRenderContext(), true);
                renderer.setSceneProvider((SceneProvider) chunky.getSceneManager());
                renderer.setSnapshotControl(new SnapshotControl() {
                    @Override
                    public boolean saveSnapshot(Scene scene, int nextSpp) {
                        return false;
                    }

                    @Override
                    public boolean saveRenderDump(Scene scene, int nextSpp) {
                        return false;
                    }
                });
                renderer.setRenderTask(renderTask);

                animationFrames.get(i).apply(scene);
                if (spp != -1) {
                    scene.setTargetSpp(spp);
                }
                scene.haltRender();
                scene.startHeadlessRender();

                renderer.start();
                renderer.join();
                renderer.shutdown();

                renderer.bufferedScene.saveFrame(new File(outputDirectory, String.format("frame%05d%s",
                        i, scene.getOutputMode().getExtension())), TaskTracker.NONE);
            }
        }

        return 0;
    }
}
