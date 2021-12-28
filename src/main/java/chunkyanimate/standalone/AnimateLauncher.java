package chunkyanimate.standalone;

import org.apache.commons.cli.*;

public class AnimateLauncher {
    public static void main(String[] args) throws Exception {
        Options options = new Options();
        options.addOption(Option.builder("h")
                .longOpt("help")
                .hasArg()
                .optionalArg(true)
                .desc("Get help, optionally for a specific option.")
                .build());
        options.addOption("r", "render", false,
                "Render an animation. `--input` and `--scene` are required. See `--help render` for more details.");
        options.addOption("t", "trim", false,
                "Trim animation frames to only contain recognized fields. See `--help trim` for more details.");
        options.addOption("k", "keyframe", false,
                "Convert keyframe into a single keyframe file. Keyframe time is taken from animation time. See `--help keyframe` for more details.");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args, true);

        if (cmd.hasOption("help")) {
            String helpCmd = cmd.getOptionValue("help");
            helpCmd = helpCmd == null ? "" : helpCmd;

            String header = "Copyright (C) 2021 Redox & Contributors";
            String footer = "\nThis program is free software; you can redistribute it and/or modify it under " +
                    "the terms of the GNU General Public License as published by the Free Software Foundation, " +
                    "license version 3.";
            HelpFormatter formatter = new HelpFormatter();
            switch (helpCmd) {
                default:
                case "":
                    formatter.printHelp("java -jar ChunkyAnimate-Standalone.jar", header, options, footer);
                    break;
                case "r":
                case "render":
                    formatter.printHelp("java -jar ChunkyAnimation-Standalone.jar --render",
                            header, AnimationRenderer.OPTIONS, footer);
                    break;
                case "t":
                case "trim":
                    formatter.printHelp("java -jar ChunkyAnimation-Standalone.jar --trim",
                            header, FrameTrimmer.OPTIONS, footer);
                    break;
                case "k":
                case "keyframe":
//                    formatter.printHelp();
                    break;
            }
            System.exit(0);
        } else if (cmd.hasOption("render")) {
            System.exit(AnimationRenderer.runRender(args));
        } else if (cmd.hasOption("trim")) {
            System.exit(FrameTrimmer.trimFrames(args));
        } else if (cmd.hasOption("keyframe")) {
//            System.exit();
        }
    }
}
