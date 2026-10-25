package comp3011;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CommandLineControllerTest {
	
	// Create a temp valid clip and inputs and pass into the Command Line controller to validate output.
    @TempDir Path tmp;

    private String video() throws IOException {
        return Files.createFile(tmp.resolve("clip.mp4")).toString();
    }

    @Test
    void stackedFlagsKeepOrderAndRepeats() throws IOException {
    	// Test 1: "-jfddn [video-file]"
    	// Filter/Frame processors testing
        var cli = new CommandLineController(new String[] {"-jfddn", video()});
        List<FrameProcessor> list = cli.getFrameProcessors();
        // 5 valid flags
        assertEquals(5, list.size());
        // Check each one is valid in their ordering
        assertInstanceOf(FrameJitterer.class, list.get(0));
        assertInstanceOf(FrameFlickerer.class, list.get(1));
        assertInstanceOf(FrameDuster.class, list.get(2));
        assertInstanceOf(FrameDuster.class, list.get(3));
        assertInstanceOf(FrameNumberer.class, list.get(4));
        
        // Make sure the two FrameDusterers are differnet
        assertNotSame(list.get(2), list.get(3));
    }

    @Test
    void launchFlagsCanBeStacked() throws IOException {
    	// Test 2: "-ax [video-file]" 
    	// No Filters, only options
        var cli = new CommandLineController(new String[] {"-ax", video()});
        // Ensure that audio and max view is set
        assertTrue(cli.isAudioRequested());
        assertTrue(cli.isMaximiseRequested());
        assertTrue(cli.getFrameProcessors().isEmpty());
    }

    @Test
    void unknownFlagKeepsFirstError() {
    	// Test 3: unknown flags
        var cli = new CommandLineController(new String[] {"-nQ"});
        assertEquals(1, cli.getExitCode());
        
        // We should get "Unknown option: Q"
        assertEquals("Unknown option: -nQ", cli.getErrorMessage());
    }

    @Test
    void shortAndLongFlags() throws IOException {
    	// Test 4: "-jfddn [video-file]"
    	// All in one test
        var cli = new CommandLineController(new String[] {"-jf", "--dust-frames", "-d", "--number-frames", "-ax", video()});
        List<FrameProcessor> list = cli.getFrameProcessors();
        
        // 7 valid flags
        assertEquals(5, list.size());
        
        // Check each one is valid in their ordering
        assertInstanceOf(FrameJitterer.class, list.get(0));
        assertInstanceOf(FrameFlickerer.class, list.get(1));
        assertInstanceOf(FrameDuster.class, list.get(2));
        assertInstanceOf(FrameDuster.class, list.get(3));
        assertInstanceOf(FrameNumberer.class, list.get(4));
        
        // Ensure audio and maximised view is true
        assertTrue(cli.isAudioRequested());
        assertTrue(cli.isMaximiseRequested());
        
        // Make sure the two FrameDusterers are differnet
        assertNotSame(list.get(2), list.get(3));
    }
}