package chunkyanimate.animation;

import it.unimi.dsi.fastutil.doubles.Double2ObjectMap;
import it.unimi.dsi.fastutil.doubles.Double2ObjectSortedMap;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import se.llbit.chunky.renderer.scene.Scene;
import se.llbit.json.JsonObject;
import se.llbit.json.JsonParser;
import se.llbit.json.JsonValue;
import se.llbit.json.PrettyPrinter;
import se.llbit.log.Log;
import se.llbit.math.QuickMath;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class AnimationUtils {
    public static JsonObject saveKeyframesJson(Double2ObjectSortedMap<AnimationKeyFrame> keyframes) {
        JsonObject kfs = new JsonObject();
        for (Double2ObjectMap.Entry<AnimationKeyFrame> keyframe : keyframes.double2ObjectEntrySet()) {
            kfs.set(Double.toString(keyframe.getDoubleKey()), keyframe.getValue().toJson());
        }
        return kfs;
    }

    public static void saveKeyframes(File out, Double2ObjectSortedMap<AnimationKeyFrame> keyframes) throws IOException {
        try (PrettyPrinter pp = new PrettyPrinter("  ",
                new PrintStream(new BufferedOutputStream(new FileOutputStream(out))))) {
            saveKeyframesJson(keyframes).prettyPrint(pp);
        }
    }

    public static void loadKeyframesJson(JsonObject in, Double2ObjectSortedMap<AnimationKeyFrame> keyframes) {
        keyframes.clear();
        for (Map.Entry<String, JsonValue> entry : in.object().toMap().entrySet()) {
            try {
                double value = Double.parseDouble(entry.getKey());
                if (entry.getValue().isObject()) {
                    keyframes.put(value, new AnimationKeyFrame(entry.getValue().asObject()));
                }
            } catch (NullPointerException | NumberFormatException e) {
                // Ignored
            }
        }
    }

    public static void loadKeyframes(File in, Double2ObjectSortedMap<AnimationKeyFrame> keyframes) throws IOException {
        try (JsonParser parser = new JsonParser(new BufferedInputStream(new FileInputStream(in)))) {
            loadKeyframesJson(parser.parse().object(), keyframes);
        } catch (JsonParser.SyntaxError e) {
            Log.error("Syntax error while loading keyframes: ", e);
        }
    }

    public static void loadFramesFromFolder(File folder, ArrayList<AnimationFrame> frames, Scene scene) {
        loadFramesFromFolder(folder, frames, scene, (progress, total) -> true);
    }

    public static void loadFramesFromFolder(File folder, ArrayList<AnimationFrame> frames, Scene scene, ProgressListener progress) {
        if (folder == null) return;

        frames.clear();
        AnimationFrame frame = new AnimationFrame(scene);

        File[] files = folder.listFiles();
        if (files == null) return;
        Arrays.sort(files, Comparator.comparing(File::getName));

        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.getName().endsWith(".json")) {
                try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
                    JsonParser parser = new JsonParser(in);
                    frame = new AnimationFrame(parser.parse().asObject(), frame);
                    frames.add(frame);
                } catch (JsonParser.SyntaxError | IOException e) {
                    Log.warn("Failed to load animation frame " + file.getName(), e);
                }
            }
            if (!progress.accept(i+1, files.length)) return;
        }
    }

    public static Map<String, PolynomialSplineFunction> interpolateKeyframes(Double2ObjectSortedMap<AnimationKeyFrame> keyframes, ProgressListener progress) {
        Map<String, PolynomialSplineFunction> interps = new HashMap<>();
        double endTime = keyframes.lastDoubleKey();
        int numKeyFrames = keyframes.size();
        int progressTotal = AnimationKeyFrame.interpolatableFields().length;
        int progressCount = 0;

        DoubleArrayList times = new DoubleArrayList(numKeyFrames);
        ArrayList<Double> entries = new ArrayList<>(numKeyFrames);
        Field[] fields = AnimationKeyFrame.interpolatableFields();
        for (Field field : fields) {
            times.clear();
            entries.clear();
            for (Double2ObjectMap.Entry<AnimationKeyFrame> keyFrameEntry : keyframes.double2ObjectEntrySet()) {
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

            if (!progress.accept(++progressCount, progressTotal)) return new HashMap<>();
        }

        return interps;
    }

    public static void loadFramesFromKeyframes(Double2ObjectSortedMap<AnimationKeyFrame> keyframes, double framerate, ArrayList<AnimationFrame> frames, Scene scene) {
        loadFramesFromKeyframes(keyframes, framerate, frames, scene, (progress, total) -> true);
    }

    public static void loadFramesFromKeyframes(Double2ObjectSortedMap<AnimationKeyFrame> keyframes, double framerate, ArrayList<AnimationFrame> frames, Scene scene, ProgressListener progress) {
        if (framerate == 0) return;
        frames.clear();

        double endTime = keyframes.lastDoubleKey();
        int numFrames = (int) (endTime * framerate) + 1;
        if (numFrames < 1) return;

        int progressTotal = AnimationKeyFrame.interpolatableFields().length + numFrames;
        int progressCount = AnimationKeyFrame.interpolatableFields().length;

        Map<String, PolynomialSplineFunction> interps =
                interpolateKeyframes(keyframes, (prog, total) -> progress.accept(prog, progressTotal));

        frames.ensureCapacity(numFrames);
        AnimationFrame frame = new AnimationFrame(scene);
        for (int i = 0; i < numFrames; i++) {
            frame = applyInterpolation(interps, i / framerate, endTime, frame);
            frames.add(frame);

            if (!progress.accept(++progressCount, progressTotal)) return;
        }
    }

    public static AnimationFrame applyInterpolation(Map<String, PolynomialSplineFunction> interp, double time, double endTime, AnimationFrame prev) {
        double safeTime = QuickMath.clamp(time, 0, endTime);
        return new AnimationFrame(field -> {
            if (interp.containsKey(field)) {
                return OptionalDouble.of(interp.get(field).value(safeTime));
            } else {
                return OptionalDouble.empty();
            }
        }, prev);
    }

    public interface ProgressListener {
        /**
         * Listen to progress and optionally cancel an operation.
         *
         * @param progress Work items completed.
         * @param total    Work items in total.
         * @return         True if work should continue. False to cancel.
         */
        boolean accept(int progress, int total);
    }
}
