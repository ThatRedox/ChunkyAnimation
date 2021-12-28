package chunkyanimate.standalone;

import chunkyanimate.animation.AnimationFrame;
import chunkyanimate.reflection.BooleanJsonField;
import chunkyanimate.reflection.DoubleJsonField;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import se.llbit.json.JsonObject;
import se.llbit.json.JsonParser;
import se.llbit.json.JsonValue;
import se.llbit.json.PrettyPrinter;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

public class FrameKeyframes {
    public static final Options OPTIONS;
    static {
        OPTIONS = new Options();
        OPTIONS.addRequiredOption("k", "keyframe", false, "Required to enter keyframe mode.");
        OPTIONS.addRequiredOption("i", "input", true, "Path to folder containing input files.");
        OPTIONS.addOption("o", "output", true, "Path to folder to put output files. Defaults to `keyframes.json`.");
        OPTIONS.addOption(null, "overwrite", false, "Overwrite output file.");
    }

    public static int fromKeyframes(String[] args) throws Exception {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(OPTIONS, args, false);

        String inputPath = cmd.getOptionValue("input");
        File inputFile = new File(inputPath);
        if (!inputFile.exists() || !inputFile.isDirectory()) {
            System.err.printf("Input must be path to a folder containing input files: %s\n", inputPath);
            return 128;
        }

        String outputPath = cmd.getOptionValue("output", "keyframes.json");
        File outputFile = new File(outputPath);
        if ((outputFile.exists() || !outputFile.isFile()) && !cmd.hasOption("overwrite")) {
            System.err.printf("Output file already exists. Use `--overwrite` to overwrite this file: %s\n", outputPath);
            return 128;
        }

        Map<String, String> knownVariables = new HashMap<>();
        for (Field field : AnimationFrame.class.getDeclaredFields()) {
            String fieldPath = null;
            BooleanJsonField booleanField = field.getAnnotation(BooleanJsonField.class);
            if (booleanField != null) {
                fieldPath = booleanField.value();
            }
            DoubleJsonField doubleField = field.getAnnotation(DoubleJsonField.class);
            if (doubleField != null) {
                fieldPath = doubleField.value();
            }

            if (fieldPath != null) {
                knownVariables.put(fieldPath, field.getName());
            }
        }

        File[] inputFiles = inputFile.listFiles();
        if (inputFiles == null) return 0;
        JsonObject keyframes = new JsonObject();
        Scanner userInput = new Scanner(System.in);
        for (File file : inputFiles) {
            if (file.getName().endsWith(".json")) {
                JsonObject frame = new JsonObject();
                try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
                    JsonParser jparse = new JsonParser(in);
                    JsonObject inFrame = jparse.parse().asObject();
                    filterPaths(inFrame, frame, new String[0], knownVariables);
                }

                double frameTime = frame.get("animationTime").doubleValue(Double.NaN);
                while (Double.isNaN(frameTime) || !keyframes.get(Double.toString(frameTime)).isUnknown()) {
                    System.out.printf("Enter frame time for %s:\n", file.getName());
                    frameTime = userInput.nextDouble();
                    if (Double.isNaN(frameTime) || !keyframes.get(Double.toString(frameTime)).isUnknown()) {
                        System.out.printf("Invalid time %f. ", frameTime);
                    }
                }

                keyframes.set(Double.toString(frameTime), frame);
            }
        }

        try (PrintStream os = new PrintStream(new BufferedOutputStream(new FileOutputStream(outputFile)))) {
            PrettyPrinter pp = new PrettyPrinter("  ", os);
            keyframes.prettyPrint(pp);
        }
        return 0;
    }

    private static boolean filterPaths(JsonObject source, JsonObject dest, String[] path, Map<String, String> knownPaths) {
        String[] newPath = Arrays.copyOf(path, path.length+1);
        boolean anyHits = false;
        for (Map.Entry<String, JsonValue> entry : source.toMap().entrySet()) {
            newPath[newPath.length-1] = entry.getKey();
            String compiledPath = compilePath(newPath);
            if (knownPaths.containsKey(compiledPath)) {
                dest.set(knownPaths.get(compiledPath), entry.getValue());
                anyHits = true;
            }

            if (entry.getValue().isObject()) {
                JsonObject tempDest = new JsonObject();
                if (filterPaths(entry.getValue().asObject(), tempDest, newPath, knownPaths)) {
                    dest.set(entry.getKey(), tempDest);
                    anyHits = true;
                }
            }
        }
        return anyHits;
    }

    private static String compilePath(String[] path) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < path.length-1; i++) {
            builder.append(path[i]);
            builder.append(".");
        }
        builder.append(path[path.length-1]);
        return builder.toString();
    }
}
