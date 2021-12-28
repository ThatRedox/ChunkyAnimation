package chunkyanimate.standalone;

import chunkyanimate.animation.AnimationFrame;
import chunkyanimate.animation.AnimationKeyFrame;
import chunkyanimate.animation.AnimationUtils;
import it.unimi.dsi.fastutil.doubles.Double2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.doubles.Double2ObjectSortedMap;
import org.apache.commons.cli.*;
import se.llbit.chunky.PersistentSettings;
import se.llbit.chunky.main.Chunky;
import se.llbit.chunky.main.ChunkyOptions;
import se.llbit.chunky.renderer.*;
import se.llbit.chunky.renderer.scene.Scene;
import se.llbit.util.TaskTracker;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;

public class AnimateLauncher {
    public static void main(String[] args) throws ParseException, IOException, InterruptedException, IllegalAccessException, NoSuchFieldException {
        Options options = new Options();
        options.addRequiredOption("i", "input", true, "Path to the input frames. Can either be a folder of frames or a JSON of keyframes.");
        options.addRequiredOption("s", "scene", true, "Scene to render. Must be path or resolvable scene name.");
        options.addOption("o", "output", true, "Path to output rendered frames.");
        options.addOption("f", "framerate", true, "Framerate to render in if using keyframes.");
        options.addOption("t", "threads", true, "Number of threads to render with.");
        options.addOption("spp", true, "Override SPP.");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        String inputFramesPath = cmd.getOptionValue("input");
        String scenePath = cmd.getOptionValue("scene");
        String outputPath = cmd.getOptionValue("output", "animation/");
        double framerate = Double.parseDouble(cmd.getOptionValue("framerate", "1.0"));
        int numThreads = Integer.parseInt(cmd.getOptionValue("threads", Integer.toString(PersistentSettings.getNumThreads())));
        int spp = Integer.parseInt(cmd.getOptionValue("spp", "-1"));

        ChunkyOptions chunkyOptions = ChunkyOptions.getDefaults();
        chunkyOptions.renderThreads = numThreads;
        Chunky chunky = new Chunky(chunkyOptions);

        Field chunkyHeadless = chunky.getClass().getDeclaredField("headless");
        chunkyHeadless.setAccessible(true);
        chunkyHeadless.set(chunky, true);

        chunky.getSceneManager().loadScene(scenePath);
        Scene scene = chunky.getSceneManager().getScene();

        ArrayList<AnimationFrame> animationFrames = new ArrayList<>();
        File inputFrames = new File(inputFramesPath);
        if (!inputFrames.exists()) {
            System.err.printf("Input frames path does not exist: %s", inputFramesPath);
            return;
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
            System.err.println("Output must be directory.");
            return;
        }

        int numFrames = animationFrames.size();
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
            System.out.printf("\n****************\nRendering frame %d out of %d.\n", i + 1, numFrames);
            try (TaskTracker.Task renderTask = taskTracker.task("Rendering")) {
                RenderManager renderer = new DefaultRenderManager(chunky.getRenderContext(), true);
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

                scene.saveFrame(new File(outputDirectory, String.format("frame%05d%s",
                        i, scene.getOutputMode().getExtension())), TaskTracker.NONE, chunky.options.renderThreads);
            }
        }
    }
}
