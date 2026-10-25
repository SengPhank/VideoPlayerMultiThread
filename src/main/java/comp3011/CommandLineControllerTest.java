package comp3011;

import java.io.File;
import java.util.List;

public class CommandLineControllerTest {

    static int failures = 0;

    static void check(boolean condition, String name) {
        if (condition) {
            System.out.println("PASS: " + name);
        } else {
            System.out.println("FAIL: " + name);
            failures++;
        }
    }

    // Turn a list of processors into "FrameNumberer FrameScratcher ..." for easy comparing.
    static String names(List<FrameProcessor> list) {
        StringBuilder sb = new StringBuilder();
        for (FrameProcessor p : list) {
            sb.append(p.getClass().getSimpleName()).append(' ');
        }
        return sb.toString().trim();
    }

    public static void main(String[] args) throws Exception {
        // Controller insists the video file really exists, so make an empty one.
        File video = File.createTempFile("clip", ".mp4");
        video.deleteOnExit();
        String path = video.getPath();

        // Test 1: order and repeats
        CommandLineController a = new CommandLineController(new String[] {"-nssd", path});
        check(names(a.getFrameProcessors())
                .equals("FrameNumberer FrameScratcher FrameScratcher FrameDuster"),
              "-nssd gives numberer, scratcher x2, duster");

        // Test 2: mixing long/short/stacked gives the same result
        CommandLineController b = new CommandLineController(new String[] {"-jfddn", path});
        CommandLineController c = new CommandLineController(
                new String[] {"-jf", "--dust-frames", "-d", "--number-frames", path});
        check(names(b.getFrameProcessors()).equals(names(c.getFrameProcessors())),
              "-jfddn equals -jf --dust-frames -d --number-frames");

        // Test 3: launch flags stack and add no processors
        CommandLineController d = new CommandLineController(new String[] {"-ax", path});
        check(d.isAudioRequested() && d.isMaximiseRequested()
                && d.getFrameProcessors().isEmpty(),
              "-ax sets audio and maximise only");

        // Test 4: bad flag is an error and reports the bad flag, not "no video file"
        CommandLineController e = new CommandLineController(new String[] {"-nQ"});
        check(e.getExitCode() == 1 && e.getErrorMessage().contains("Q"),
              "-nQ fails and names the bad flag");

        System.out.println(failures == 0 ? "ALL PASSED" : failures + " FAILED");
    }
}