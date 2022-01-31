package dev.thatredox.chunky.animate.standalone;

import dev.thatredox.chunky.animate.animation.AnimationFrame;
import dev.thatredox.chunky.animate.reflection.BooleanJsonField;
import dev.thatredox.chunky.animate.reflection.DoubleJsonField;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FrameTrimmer {
    public static final Options OPTIONS;
    static {
        OPTIONS = new Options();
        OPTIONS.addRequiredOption("t", "trim", false, "Required to enter trimming mode.");
        OPTIONS.addRequiredOption("i", "input", true, "Path to folder containing input files.");
        OPTIONS.addOption("o", "output", true, "Path to folder to put output files. Default overwrites input files.");
    }

    public static int trimFrames(String[] args) throws Exception {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(OPTIONS, args, false);

        String inputPath = cmd.getOptionValue("input");
        File inputFile = new File(inputPath);
        if (!inputFile.exists() || !inputFile.isDirectory()) {
            System.err.printf("Input must be path to a folder containing input files: %s\n", inputPath);
            return 128;
        }

        String outputPath = cmd.getOptionValue("output", null);
        File outputFolder = null;
        if (outputPath != null) {
            outputFolder = new File(outputPath);
            outputFolder.mkdirs();
            if (!outputFolder.isDirectory()) {
                System.err.printf("Output must be a path to a folder: %s\n", outputPath);
                return 128;
            }
        }

        Set<String> knownPaths = new HashSet<>();
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
                knownPaths.add(fieldPath);
            }
        }

        File[] inputFiles = inputFile.listFiles();
        if (inputFiles == null) return 0;
        for (File file : inputFiles) {
            if (file.getName().endsWith(".json")) {
                JsonObject outFrame = new JsonObject();
                try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
                    JsonParser jparse = new JsonParser(in);
                    JsonObject inFrame = jparse.parse().asObject();
                    filterPaths(inFrame, outFrame, new String[0], knownPaths);
                }

                File outFile;
                if (outputPath == null) {
                    outFile = file;
                } else {
                    outFile = new File(outputFolder, file.getName());
                }
                try (PrintStream os = new PrintStream(new BufferedOutputStream(new FileOutputStream(outFile)))) {
                    PrettyPrinter pp = new PrettyPrinter("  ", os);
                    outFrame.prettyPrint(pp);
                }
            }
        }
        return 0;
    }

    private static boolean filterPaths(JsonObject source, JsonObject dest, String[] path, Set<String> knownPaths) {
        String[] newPath = Arrays.copyOf(path, path.length+1);
        boolean anyHits = false;
        for (Map.Entry<String, JsonValue> entry : source.toMap().entrySet()) {
            newPath[newPath.length-1] = entry.getKey();
            String compiledPath = compilePath(newPath);
            if (knownPaths.contains(compiledPath)) {
                dest.set(entry.getKey(), entry.getValue());
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
