/*
 * Starter code supplied for Adelaide University COMP3011 Assignment 2.
 * Students are free to modify this file for assessment purposes.
 * 
 * Authors:
 *   1. Simon Ratcliffe, in collaboration with GPT-5.6 Terra
 *   2. Sengphachanh Phank a1967663
 *
 * Copyright 2026 Simon Ratcliffe
 */
package comp3011;

import java.io.File;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.JavaFXFrameConverter;

import javafx.animation.AnimationTimer;
import javafx.scene.image.Image;

/**
 * The model part of the video player model-view-controller architecture.
 *
 * <p>
 * Owns the FFmpeg decoder, playback clock, seeking logic, audio scheduling,
 * and frame processing - which it all manages through decoding, effects, 
 * and conversion on multiple threads. It publishes decoded frames and playback state through
 * callbacks supplied by its owning {@link VideoPlayerController}, keeping it
 * decoupled from the JavaFX view.
 * </p>
 */
public class VideoPlayerModel {
    private static final long NO_SEEK_REQUEST = -1; // Sentinel value used when no seek position is active.
    private static final long FIVE_SECONDS_US = 5_000_000L;
    private static final long AUDIO_LEAD_NS = 30_000_000L;
    private static final int PREBUFFER_FRAMES = 3; // frames queued before the playback clock starts
    
    private final List<FrameProcessor> frameProcessors;
    private final Queue<PendingAudio> pendingAudio = new ConcurrentLinkedQueue<>();
    private final BiConsumer<Integer, Integer> videoSizeChangedHandler;
    private final Consumer<Image> frameReadyHandler;
    private final Consumer<String> statusChangedHandler;
    private final BiConsumer<Boolean, Boolean> playbackStateChangedHandler;
    private final Consumer<Boolean> audioOutputStateChangedHandler;
    private final Consumer<Boolean> subtitleStateChangedHandler;
    private final BiConsumer<Long, Long> timeChangedHandler;
    private final ToggleableProcessor subtitleProcessor;
    
    private FramePipeline pipeline;
    private boolean needFrameWhilePaused;
    
    // Cuebank for subtitle handling
    private volatile CueBank cueBank;
    private Cue lastCue;
    private SubtitleRenderer.Strip strip;
    
    // Audio and subtitle chunking (for now, its 20s)
    private static final long CHUNK_DURATION_US = 20_000_000L;
    private Thread subtitleLoaderThread;
    private static final long RETRY_DELAY_MS = 2_000L;
    private volatile long priorityChunkUs = NO_SEEK_REQUEST;
    private volatile boolean videoEnded = false;
    
    public void setCueBank(CueBank cueBank) {
        this.cueBank = cueBank;
    }
    
    // This is the critical wiring that allows the framework (JavaFX) to call
    // into our model logic every time it goes around its event loop. Every GUI
    // system has an event loop so that button clicks, key presses and window
    // resizes can be responded to. Most offer ways of running additional logic
    // in either idle time (when there are no pending UI events to process) or
    // on a regular heart beat, such as with this JavaFX AnimationTimer. Here
    // we just create a little anonymous local subclass and override the handle
    // method to call the method we want run every heart beat.
    private final AnimationTimer playbackTimer = new AnimationTimer() {
        @Override
        public void handle(long now) {
            pumpPlayback(now);
        }
    };

    private File videoFile; // Path to the media that comes from the command line
    private FFmpegFrameGrabber grabber; // This is the 3rd party video media decoder. It deals in JavaCV Frame objects.
    private JavaFXFrameConverter converter; // Takes Frame objects to JavaFX Image objects, which can be put on screen.
    private AudioPlayer audioPlayer; // This is ours. It has some real time buffering smarts.
    private PreparedFrame preparedFrame; // Ours, but is just an Image with some meta-data added.
    private boolean playbackOpen; // True when playback is happening.
    private boolean pauseRequested;
    private boolean audioOutputEnabled;
    private boolean audioAvailable;
    private boolean subtitlesEnabled;
    private long currentTimestampUs;
    private long videoDurationUs = NO_SEEK_REQUEST;
    private int videoFrameDurationUs;
    private double frameRate;
    private int intFrameRate;
    private int totalVideoFrames;
    private long firstTimestampUs = NO_SEEK_REQUEST;
    private long logicalPlaybackBaseUs;
    private long playbackStartNs;
    private long pauseStartedNs;
    private long relativeSeekBaseUs = NO_SEEK_REQUEST;

    public VideoPlayerModel(
            boolean audioEnabled,
            List<FrameProcessor> frameProcessors,
            BiConsumer<Integer, Integer> videoSizeChangedHandler,
            Consumer<Image> frameReadyHandler,
            Consumer<String> statusChangedHandler,
            BiConsumer<Boolean, Boolean> playbackStateChangedHandler,
            Consumer<Boolean> audioOutputStateChangedHandler,
            Consumer<Boolean> subtitleStateChangedHandler,
            BiConsumer<Long, Long> timeChangedHandler,
            ToggleableProcessor subtitleProcessor) {
        audioOutputEnabled = audioEnabled;
        this.videoSizeChangedHandler = videoSizeChangedHandler;
        this.frameReadyHandler = frameReadyHandler;
        this.statusChangedHandler = statusChangedHandler;
        this.playbackStateChangedHandler = playbackStateChangedHandler;
        this.audioOutputStateChangedHandler = audioOutputStateChangedHandler;
        this.subtitleStateChangedHandler = subtitleStateChangedHandler;
        this.frameProcessors = List.copyOf(frameProcessors);
        this.subtitleProcessor = subtitleProcessor;
        this.timeChangedHandler = timeChangedHandler;
    }

    public void play(File file) {
        videoFile = file;
        videoDurationUs = NO_SEEK_REQUEST;
        videoFrameDurationUs = 0;
        loadSubtitles();
        startPlayback(0, false, NO_SEEK_REQUEST);
    }

    public void startOver() {
        if (videoFile == null) {
            return;
        }

        seekTo(0);
    }

    public void backFiveSeconds() {
        seekRelative(-FIVE_SECONDS_US);
    }

    public void forwardFiveSeconds() {
        seekRelative(FIVE_SECONDS_US);
    }

    public void togglePause() {
        if (!playbackOpen) {
            if (videoFile != null) {
            	// If video ends (or we pulled the usb), restart the video
            	long start = displayableSeekTimestamp(currentTimestampUs);
            	long currentStamp = currentTimestampUs;
            	if (videoEnded) {
            		start = 0;
            		currentStamp = NO_SEEK_REQUEST;
                	videoEnded = false;
            	}
            	
                startPlayback(start, false, currentStamp);
            }
            return;
        }

        pauseRequested = !pauseRequested;
        relativeSeekBaseUs = NO_SEEK_REQUEST;

        if (pauseRequested) {
            pauseStartedNs = System.nanoTime();
            flushAudioOutput();
        } else {
            resumePlaybackClock(System.nanoTime());
        }

        notifyPlaybackStateChanged();
    }

    public void toggleAudioOutput() {
        audioOutputEnabled = !audioOutputEnabled;
        pendingAudio.clear();
        flushAudioOutput();
        notifyAudioOutputStateChanged();
    }

    public boolean isAudioOutputEnabled() {
        return audioOutputEnabled;
    }

    public void stopPlayback() {
        closePlaybackResources();
        currentTimestampUs = 0;
        pauseRequested = false;
        notifyStatusChanged("Stopped");
        notifyFrameReady(null);
        notifyPlaybackStateChanged();
        notifyTimeChanged();
    }
    
    public void toggleSubtitles() {
    	subtitleProcessor.toggle();
        notifySubtitleStateChanged();
    }
    
    public boolean isSubtitlesEnabled() {
        return subtitleProcessor.isEnabled();
    }
    
    public void shutdown() {
        closePlaybackResources();
    }

    private void seekRelative(long offsetUs) {
        if (videoFile == null) {
            return;
        }

        long baseTimestampUs = relativeSeekBaseUs != NO_SEEK_REQUEST
                ? relativeSeekBaseUs
                : currentTimestampUs;
        seekTo(baseTimestampUs + offsetUs);
    }
    
    public void seekTo(long timestampUs) {
    	if (videoFile == null || !videoFile.exists()) {
            handlePlaybackError(new Exception("Could not find video file"));
            return;
        }    	
        long logicalTimestampUs = clampSeekTimestamp(timestampUs);
        long grabTimestampUs = displayableSeekTimestamp(logicalTimestampUs);
        
        priorityChunkUs = (logicalTimestampUs / CHUNK_DURATION_US) * CHUNK_DURATION_US;
        videoEnded = false;
        
        if (!playbackOpen) {
            startPlayback(grabTimestampUs, pauseRequested, logicalTimestampUs);
            return;
        }

        try {
        	// Pause frame filter generation when seeking
            pipeline.stop();
            
            grabber.setTimestamp(grabTimestampUs);
            resetPlaybackClock(logicalTimestampUs);
            flushAudioOutput();
            
            // restart the pipeline when finished
            pipeline.start();
            
            notifyPlaybackStateChanged();
        } catch (Exception e) {
            handlePlaybackError(e);
        }
    }

    private void startPlayback(long startTimestampUs, boolean initiallyPaused, long initialRelativeSeekBaseUs) {
        closePlaybackResources();

        pauseRequested = initiallyPaused;
        currentTimestampUs = initialRelativeSeekBaseUs != NO_SEEK_REQUEST
                ? initialRelativeSeekBaseUs
                : startTimestampUs;
        relativeSeekBaseUs = initialRelativeSeekBaseUs;

        notifyStatusChanged(videoFile.getName());
        
        if (videoFile == null || !videoFile.exists()) {
            handlePlaybackError(new Exception("Could not find video file"));
            return;
        }
        
        try {
            openPlaybackResources(startTimestampUs);
            resetPlaybackClock(currentTimestampUs);
            playbackOpen = true;
            pipeline = new FramePipeline(grabber, converter, frameProcessors, mediaName(videoFile), audioAvailable, audioPlayer, pendingAudio);
            pipeline.start();
            playbackTimer.start();
            notifyPlaybackStateChanged();
        } catch (Exception e) {
            handlePlaybackError(e);
        }
    }

    private void openPlaybackResources(long startTimestampUs) throws Exception {
        converter = new JavaFXFrameConverter();
        grabber = new FFmpegFrameGrabber(videoFile);
        grabber.setImageMode(FrameGrabber.ImageMode.COLOR);
        grabber.setSampleMode(FrameGrabber.SampleMode.SHORT);
        grabber.start();

        notifyVideoSizeChanged(grabber.getImageWidth(), grabber.getImageHeight());

        frameRate = grabber.getFrameRate();
        intFrameRate = (int) Math.round(frameRate);
        videoFrameDurationUs = frameRate > 0
                ? (int) Math.round(1_000_000.0 / frameRate)
                : 0;
        totalVideoFrames = grabber.getLengthInVideoFrames();

        long durationUs = grabber.getLengthInTime();
        videoDurationUs = durationUs > 0
                ? durationUs
                : NO_SEEK_REQUEST;

        audioAvailable = grabber.hasAudio();
        audioPlayer = new AudioPlayer();
        if (audioAvailable) {
            audioPlayer.open(grabber.getSampleRate(), grabber.getAudioChannels());
        }

        if (startTimestampUs > 0) {
            grabber.setTimestamp(startTimestampUs);
        }
    }

    private void resetPlaybackClock(long logicalTimestampUs) {
        pendingAudio.clear();
        preparedFrame = null;
        currentTimestampUs = logicalTimestampUs;
        relativeSeekBaseUs = logicalTimestampUs;
        firstTimestampUs = NO_SEEK_REQUEST;
        logicalPlaybackBaseUs = logicalTimestampUs;
        playbackStartNs = 0;
        pauseStartedNs = pauseRequested ? System.nanoTime() : 0;
        needFrameWhilePaused = true;
        
        notifyTimeChanged();
    }

    private void resumePlaybackClock(long now) {
        if (pauseStartedNs > 0 && playbackStartNs > 0) {
            playbackStartNs += now - pauseStartedNs;
        }
        pauseStartedNs = 0;
    }

    // This is called on a heart beat by the GUI thread. We want to be co-operative here by (a) not blocking, and (b)
    // getting our required work out of the way quickly so that we can return control flow to the JavaFX event loop for
    // handling user interaction with the GUI and rendering. We have three jobs: (1) keep audio flowing if sound is on,
    // (2) display a frame if it is due and (3) prepare the next frame if we're in the window after the previous frame
    // has gone to the display. We don't buffer frames here, just handling them one at a time.
    private void pumpPlayback(long now) {
        if (!playbackOpen) {
            return;
        }

        // Task (1)
        writeDueAudio(now);
      
        // Handle pipeline error before task 2 and 3
        Exception failure = pipeline.getFailure();
        if (failure != null) {
            handlePlaybackError(failure);
            return;
        }
        
        // Task (2)
        if (preparedFrame != null && targetTimeNs(preparedFrame)  <= now) {
            // We have a prepared frame ready to go and it is due (or just past due!) so get it up on screen ASAP!
            displayPreparedFrame(now);
        }

        // Task (3). We'll try and be nice to the GUI event loop here by queueing the prepareNextFrame call rather than
        // hogging the thread and doing it here, hence the tricky callback and use of prepareNextFrameQueued. Feel free
        // to replace this whole block with simple linear control flow logic
        // such as: if (!pauseRequested && preparedFrame == null) prepareNextFrame();
        // to compare.
        if (preparedFrame == null && (!pauseRequested || needFrameWhilePaused)) {
            boolean ended = pipeline.isFinished();
            
            // pre-buffer before starting the clock
            if (firstTimestampUs == NO_SEEK_REQUEST && !ended && pipeline.readyCount() < PREBUFFER_FRAMES) { return; }
            preparedFrame = pipeline.pollFrame();
        	
        	if (preparedFrame == null) {
                if (ended) { finishPlayback(); }
                return;
            }
        	
        	// the clock starts on the first frame we receive
        	if (firstTimestampUs == NO_SEEK_REQUEST) {
                firstTimestampUs = preparedFrame.mediaTimestampUs();
                playbackStartNs = System.nanoTime();
                if (pauseRequested) { pauseStartedNs = playbackStartNs; }
            }
        }
    }

    private void displayPreparedFrame(long now) {
        PreparedFrame frame = preparedFrame;
        preparedFrame = null;
        currentTimestampUs = logicalTimestampUs(frame);
        relativeSeekBaseUs = NO_SEEK_REQUEST;

        // Dump some logging to the console once per second so that real time performance can be monitored.
        if (intFrameRate > 0 && frame.frameNumber() % intFrameRate == 0) {
            long remainingUs = (targetTimeNs(frame) - frame.preparedAtNs()) / 1_000L;
            if (remainingUs < 0) {
                System.out.printf("Frame headroom is \u001B[31m%5dus\u001B[0m out of %dus spare.%n",
                        remainingUs,
                        videoFrameDurationUs);
            } else {
                System.out.printf("Frame headroom is \u001B[32m%5dus\u001B[0m out of %dus spare.%n",
                        remainingUs,
                        videoFrameDurationUs);
            }
        }
        
        needFrameWhilePaused = false;
        notifyFrameReady(frame.image());
        notifyTimeChanged();
    }

    private void writeDueAudio(long now) {
        // Handle some obvious early exits
        if (!audioOutputEnabled || audioPlayer == null) {
            pendingAudio.clear();
            return;
        }
        if (pauseRequested || firstTimestampUs == NO_SEEK_REQUEST || playbackStartNs <= 0) {
            return;
        }

        // Here is the real time dependent logic
        long dueTimestampUs = firstTimestampUs + (now + AUDIO_LEAD_NS - playbackStartNs) / 1_000L;
        while (!pendingAudio.isEmpty()) {
            PendingAudio audio = pendingAudio.peek();
            if (audio.timestampUs() > dueTimestampUs) {
                return;
            }

            int written = audioPlayer.write(audio.samples(), audio.offset(), audio.remaining());
            if (written <= 0) {
                return;
            }

            audio.advance(written);
            if (!audio.finished()) {
                return;
            }
            pendingAudio.remove();
        }
    }

    private void finishPlayback() {
        long endTimestampUs = videoDurationUs != NO_SEEK_REQUEST
                ? videoDurationUs
                : currentTimestampUs;

        closePlaybackResources();
        videoEnded = true;
        pauseRequested = true;
        currentTimestampUs = endTimestampUs;
        relativeSeekBaseUs = endTimestampUs;
        notifyStatusChanged("Playback finished");
        notifyPlaybackStateChanged();
    }

    private void handlePlaybackError(Exception e) {
        // e.printStackTrace();
    	System.err.println("Playback error: " + e.getMessage());
        closePlaybackResources();
        pauseRequested = false;
        notifyStatusChanged("Error: " + e.getMessage());
        notifyPlaybackStateChanged();
    }

    private void closePlaybackResources() {
    	if (pipeline != null) {
    		pipeline.stop();
    		pipeline = null;
    	}
        playbackTimer.stop();
        playbackOpen = false;
        preparedFrame = null;
        pendingAudio.clear();
        firstTimestampUs = NO_SEEK_REQUEST;
        playbackStartNs = 0;
        pauseStartedNs = 0;
        audioAvailable = false;

        if (audioPlayer != null) {
            audioPlayer.close();
            audioPlayer = null;
        }

        if (grabber != null) {
            try {
                grabber.stop();
            } catch (Exception e) {
                // The resource is being closed; there is no useful recovery action.
            }
            try {
                grabber.close();
            } catch (Exception e) {
                // The resource is being closed; there is no useful recovery action.
            }
            grabber = null;
        }

        if (converter != null) {
            converter.close();
            converter = null;
        }
    }

    private String mediaName(File file) {
        String fileName = file.getName();
        int extensionStart = fileName.lastIndexOf('.');
        if (extensionStart <= 0) {
            return fileName;
        }
        return fileName.substring(0, extensionStart);
    }
    
    /* Fetches the subtitles chunk for the specific time frame.
     * Uses a background thread to fetch cues, so video player doesnt lag
     * 
     */
    private void loadSubtitles() {
        if (!(subtitleProcessor.getProcessor() instanceof FrameSubtitle subtitles)) {
            return;
        }
        if (subtitleLoaderThread != null) {
            subtitleLoaderThread.interrupt();
        }

        File targetFile = videoFile;
        String videoName = targetFile.getName();
        CueBank bank = new CueBank();
        subtitles.setCueBank(bank);

        subtitleLoaderThread = new Thread(() -> {
            SubtitleClient client = new SubtitleClient();
            java.util.Set<Long> loadedChunks = new java.util.HashSet<>();
            long offsetUs = 0;

            while (!Thread.currentThread().isInterrupted()) {
            	// If the user seeks at a timestamp, jump straight to that chunk
                long requested = priorityChunkUs;
                if (requested != NO_SEEK_REQUEST) {
                    priorityChunkUs = NO_SEEK_REQUEST;
                    offsetUs = requested;
                }
                
                // Skip chunks we already have
                if (loadedChunks.contains(offsetUs)) {
                    offsetUs += CHUNK_DURATION_US;
                    continue;
                }
                
                // If the USB was unplugged, wait until it comes back instead of killing
                if (!targetFile.exists()) {
                    if (!sleepQuietly(RETRY_DELAY_MS)) break;
                    continue;
                }
                
                try {
                    byte[] mp3Chunk = AudioExtractor.extractMp3Chunk(targetFile, offsetUs, CHUNK_DURATION_US);
                    if (mp3Chunk.length == 0) {
                    	// Reached the end of the file; wait in case the user seeks back to an unloaded chunk
                        if (!sleepQuietly(500)) break;
                        break; // reached end of audio
                    }

                    List<Cue> cues = client.fetchCues(mp3Chunk, videoName, offsetUs);
                    bank.addAll(cues);
                    loadedChunks.add(offsetUs);
                    offsetUs += CHUNK_DURATION_US;
                } catch (Exception e) {
                    System.err.println("Subtitle chunk failed at " + offsetUs + "us (retrying): " + e.getMessage());
                    if (!sleepQuietly(RETRY_DELAY_MS)) break;
                }
            }
        }, "subtitle-loader");
        subtitleLoaderThread.setDaemon(true);
        subtitleLoaderThread.start();
    }
    
    // Was able to sleep peacefully without killing?
    private boolean sleepQuietly(long millis) {
    	try {
    		Thread.sleep(millis);
    		return true;
    	} catch (InterruptedException e) {
    		Thread.currentThread().interrupt();
    		return false;
    	}
    }
    private long clampSeekTimestamp(long timestampUs) {
        long clampedTimestampUs = Math.max(0, timestampUs);
        long durationUs = videoDurationUs;
        if (durationUs != NO_SEEK_REQUEST) {
            clampedTimestampUs = Math.min(clampedTimestampUs, durationUs);
        }
        return clampedTimestampUs;
    }

    private long displayableSeekTimestamp(long logicalTimestampUs) {
        long durationUs = videoDurationUs;
        if (durationUs == NO_SEEK_REQUEST || logicalTimestampUs < durationUs) {
            return logicalTimestampUs;
        }

        int frameDurationUs = videoFrameDurationUs;
        if (frameDurationUs <= 0) {
            return Math.max(0, durationUs - 1);
        }

        return Math.max(0, durationUs - frameDurationUs);
    }
    
    private void flushAudioOutput() {
        if (audioPlayer != null) {
            audioPlayer.flush();
        }
    }

    private void notifyVideoSizeChanged(int width, int height) {
        videoSizeChangedHandler.accept(width, height);
    }

    private void notifyFrameReady(Image image) {
        frameReadyHandler.accept(image);
    }

    private void notifyStatusChanged(String status) {
        statusChangedHandler.accept(status);
    }

    private void notifyPlaybackStateChanged() {
        playbackStateChangedHandler.accept(playbackOpen, pauseRequested);
    }

    private void notifyAudioOutputStateChanged() {
        audioOutputStateChangedHandler.accept(audioOutputEnabled);
    }
    
    private void notifySubtitleStateChanged() {
        subtitleStateChangedHandler.accept(subtitleProcessor.isEnabled());
    }

//    private record PreparedFrame(
//            Image image,
//            int frameNumber,
//            long mediaTimestampUs,
//            long logicalTimestampUs,
//            long targetTimeNs,
//            long preparedAtNs) {
//    }
    
    // return the exact time each frame should appear
    private long targetTimeNs(PreparedFrame f) {
    	long timeNs = playbackStartNs + Math.max(0, f.mediaTimestampUs() - firstTimestampUs) * 1_000L;
    	return timeNs;
    }
    // calculate logical playback position relative to video timeline (for seeking)
    private long logicalTimestampUs(PreparedFrame f) {
    	long timeUs = logicalPlaybackBaseUs + Math.max(0, f.mediaTimestampUs() - firstTimestampUs);
        return timeUs;
    }
    // Update the video time stamp when changing time stamps
    private void notifyTimeChanged() {
        timeChangedHandler.accept(currentTimestampUs, Math.max(0, videoDurationUs));
    }
    
}
