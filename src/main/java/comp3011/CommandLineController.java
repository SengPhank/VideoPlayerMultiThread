/*
 * Starter code supplied for Adelaide University COMP3011 Assignment 2.
 * Students are free to modify this file for assessment purposes.
 * 
 * Authors:
 *   1. Simon Ratcliffe, in collaboration with GPT-5.6 Terra
 *   2. Sengphachanh Phank. a1967663
 *
 * Copyright 2026 Simon Ratcliffe
 */
package comp3011;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.Map;
import java.util.HashMap;
/**
 * Created in the VideoPlayerApp's main function to receive and parse the video player's command-line arguments.
 *
 * <p>
 * Extracts the video file and supported launch options, reports invalid input
 * or help text, and exposes the resulting launch configuration via methods to the
 * {@link VideoPlayerApp}.
 * </p>
 */
public class CommandLineController {
    private final String[] args;

    private boolean helpRequested;
    private boolean audioRequested;
    private boolean maximiseRequested;
    private Integer displayId;
    private File videoFile;
    private String errorMessage;
    
    // FrameProcessor data
    private record ProcessorOption(char shortFlag, String longFlag, String description, Supplier<FrameProcessor> factory) {}
    
    // add a new effect here
    private static final List<ProcessorOption> PROCESSOR_OPTIONS = List.of(
	    new ProcessorOption('n', "number-frames", "Render the frame number onto each frame", FrameNumberer::new),
	    new ProcessorOption('s', "scratch-frames", "Render vertical film scratches", FrameScratcher::new),
	    new ProcessorOption('f', "flicker-frames", "Randomly dim frames", FrameFlickerer::new),
	    new ProcessorOption('w', "black-and-white", "Convert frames to black and white", FrameBlackAndWhiter::new),
	    new ProcessorOption('y', "yellow-frames", "Apply a warmer colour temperature", FrameYellower::new),
	    new ProcessorOption('v', "vignette-frames", "Darken the frame edges", FrameVignetter::new),
	    new ProcessorOption('d', "dust-frames", "Render dust and hair marks", FrameDuster::new),
	    new ProcessorOption('j', "jitter-frames", "Randomly displace frames by a few pixels", FrameJitterer::new),
	    new ProcessorOption('m', "mottle-frames", "Add cloudy emulsion mottling", FrameMottler::new),
	    new ProcessorOption('b', "bleed-frames", "Bleed light into frames", FrameBleeder::new),
	    new ProcessorOption('p', "pepper-frames", "Pepper frames with dark spots/blotches", FramePepperer::new)
	);
    		
    // Fast lookup tables
    private static final Map<Character, ProcessorOption> shortFlagLookup = new HashMap<>();
    private static final Map<String, ProcessorOption> longFlagLookup = new HashMap<>();
    
    // Formatting print statements
    private static int longestFlagSize = 0;
   
    
    // Initialise Table and other variables at run time
    static {
    	for (ProcessorOption option : PROCESSOR_OPTIONS) {
            shortFlagLookup.put(option.shortFlag(), option);
            longFlagLookup.put(option.longFlag(), option);
            longestFlagSize = Math.max(longestFlagSize, option.longFlag().length());
        }
    }
    
    // Final filters request
    private final List<FrameProcessor> frameProcessors = new ArrayList<>();
    
    // Constructor ===
    public CommandLineController(String[] args) {
        this.args = args.clone();
        parse();
        if (errorMessage != null) {
            System.out.println(errorMessage);
        }
        
        if (helpRequested) {
            printHelp();
        }
    }
    
    // Methods ===    
    public File getVideoFile() {
        return videoFile;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getExitCode() {
        return errorMessage == null ? 0 : 1;
    }

    public Integer getDisplayId() {
        return displayId;
    }

    public boolean isAudioRequested() {
        return audioRequested;
    }

    public boolean isMaximiseRequested() {
        return maximiseRequested;
    }

    public boolean shouldLaunchApplication() {
        return errorMessage == null && videoFile != null;
    }

    private void parse() {
        List<String> videoFiles = new ArrayList<>();
        
        for (String arg : args) {
            if (arg.startsWith("--")) {
                parseLongOption(arg);
            } else if (arg.startsWith("-")) {
                parseShortOptions(arg);
            } else {
                videoFiles.add(arg);
            }
        }

        if (videoFiles.size() > 1) {
        	setError("Usage: VideoPlayer [options] [video-file]");
        } else if (videoFiles.size() == 1) {
            videoFile = new File(videoFiles.get(0));
            if (!videoFile.isFile()) {
            	setError("File not found: " + videoFile.getPath());
                videoFile = null;
            }
        } else {
            if (!helpRequested) {
            	setError("No video file specified.");
            }
        }
        
        // print test
//        if (errorMessage == null) {
//        	System.out.println("Valid frame processors: " + frameProcessors.size());
//        }
    }
    
    private void parseShortOptions(String arg) {
    	// Edge case
    	if (arg.length() == 1) { setError("Unknown option: " + arg); return; }
    	
    	for (char flag : arg.substring(1).toCharArray()) {
    		switch (flag) {
    			case 'h' -> helpRequested = true;
    			case 'a' -> audioRequested = true;
    			case 'x' -> maximiseRequested = true;
    			case '1' -> setDisplayId(1);
    			case '2' -> setDisplayId(2);
    			default -> addProcessor(shortFlagLookup.get(flag), arg);
    		}
    	}
    	
    }
        
    private void parseLongOption(String arg) {
    	// Edge case
    	if (arg.length() == 2) { setError("Unknown option: " + arg); return; }
    	String flag = arg.substring(2);
    	
		switch (flag) {
			case "help" -> helpRequested = true;
			case "audio" -> audioRequested = true;
			case "maximise" -> maximiseRequested = true;
			case "monitor-1" -> setDisplayId(1);
			case "monitor-2" -> setDisplayId(2);
			default -> addProcessor(longFlagLookup.get(flag), arg);
		}
    	
    }
    
    private void addProcessor(ProcessorOption option, String token) {
        if (option == null) { setError("Unknown option: " + token); return; }
        frameProcessors.add(option.factory().get());
    }
    private void setDisplayId(int displayId) {
        if (this.displayId != null && this.displayId != displayId) {
        	setError("Only one display option can be used");
            return;
        }
        this.displayId = displayId;
    }

    private void printHelp() {
        System.out.println("Usage: VideoPlayer [options] [video-file]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -h, --help         Show this help message");
        System.out.println("  -a, --audio        Play audio");
        System.out.println("  -x, --maximise     Open the player maximised");
        System.out.println("  -1, --monitor-1    Open the player on display 1");
        System.out.println("  -2, --monitor-2    Open the player on display 2");
        
        
        // Print frame processor. Has a nice formatted output, where description all starts on the same column.
        System.out.println("\nFrame processors:");
        for (ProcessorOption opt : PROCESSOR_OPTIONS) {
        	int whiteSpaces = longestFlagSize - opt.longFlag().length() + 2;
        	System.out.print("  -" + opt.shortFlag() + ", --" + opt.longFlag());
        	while (whiteSpaces > 0) { System.out.print(' '); whiteSpaces -= 1; }
        	System.out.println(opt.description());
        }
        
        System.out.println("\nFrame processors are applied in command-line order and may be repeated.");
        System.out.println("Example: -nssnfwyvdjmbp numbers, scratches twice, numbers again, flickers,");
        System.out.println("converts, warms, vignettes, dusts, jitters, mottles, bleeds, then peppers.");
    }
    
    // Return filter request
    public List<FrameProcessor> getFrameProcessors() {
    	return List.copyOf(frameProcessors);
    }
    
    // Change the error message to first-found error
    private void setError(String msg) {
    	if (errorMessage == null) { errorMessage = msg; }
    }
}
