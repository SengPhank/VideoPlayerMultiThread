package comp3011;


import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import javafx.scene.image.Image;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.JavaFXFrameConverter;
import org.bytedeco.javacv.Frame;


public class FramePipeline {
	// Attributes
	private volatile boolean running;
	
	// Thread stuff
	private Thread thread;
	private volatile boolean finished;
	private volatile Exception failure;
	private final ArrayBlockingQueue<PreparedFrame> readyFrame = new ArrayBlockingQueue<PreparedFrame>(4);
	
	private final FFmpegFrameGrabber grabber;
	private final JavaFXFrameConverter converter;
	private final List<FrameProcessor> frameProcessors;
	private final String mediaName;
	private final boolean audioAvailable;
	private boolean frameProcessorsInitialised = false;
	
	// Methods====
	
	// Takes the frame grabber, converter and processors from VideoPlayerModel
	FramePipeline (FFmpegFrameGrabber grabber, JavaFXFrameConverter converter, List<FrameProcessor> frameProcessors, String mediaName, boolean audioAvailable) {
		this.grabber = grabber;
		this.converter = converter;
		this.frameProcessors = frameProcessors;
		this.mediaName = mediaName;
		this.audioAvailable = audioAvailable;
	}
	
	public boolean isFinished() {
        return finished;
    }
	
	public Exception getFailure() {
        return failure;
    }
	
	// Creates and start worker threads for processing filters (one for now)
	public void start() {
		running = true;
		finished = false;
		failure = null;
		thread = new Thread(this::run, "frame-pipeline");
		thread.setDaemon(true);
		thread.start();
	}
	
	// Stops the current thread from doing any more work (used for stop, seek and close)
	public void stop() {
		running = false;
		if (thread != null) {
			// Give some time for UI to close before clearing all preparedFrames
			thread.interrupt();
			
			try { 
				thread.join(2000);
			}
			catch (InterruptedException e) { 
				Thread.currentThread().interrupt();
			}
			thread = null;
		}
		readyFrame.clear();
	}
	
	// returns the next ready frame, or "null" if nothing is available
	public PreparedFrame pollFrame() {
		return readyFrame.poll();
	}
	
	// run the thread pipeline, creating the filtered frames and sending it to the video player.
	private void run() {
		
		
		try {
			while (running) {
	            Frame frame = grabFrame();
	            if (frame == null) {
	                finished = true;
	                return;
	            }
	
	            long timestampUs = grabber.getTimestamp();
	            // TODO audio
//	            if (audioAvailable && frame.samples != null) {
//	                // queueAudio(timestampUs, frame);
//	            }
	
	            if (frame.image == null) {
	                continue;
	            }
	            
	            if (!frameProcessorsInitialised) {
	            	int totalVideoFrames = grabber.getLengthInVideoFrames();
		            double frameRate = grabber.getFrameRate();
		            int intFrameRate = (int) Math.round(frameRate);
		            int videoFrameDurationUs = frameRate > 0
		                    ? (int) Math.round(1_000_000.0 / frameRate)
		                    : 0;
		            
	                initialiseFrameProcessors(new InfoVideo(
	                        mediaName,
	                        totalVideoFrames,
	                        frame.imageWidth,
	                        frame.imageHeight,
	                        frame.imageDepth,
	                        frame.imageChannels,
	                        frame.imageStride,
	                        frameRate,
	                        intFrameRate,
	                        videoFrameDurationUs,
	                        grabber.getPixelFormat()));
	                frameProcessorsInitialised = true;
	            }
	
	
	            int frameNumber = grabber.getFrameNumber();
	            InfoFrame info = new InfoFrame(frameNumber, timestampUs);
	            processFrame(frame, info);
	            
	            Image image = converter.convert(frame);
	            
	            PreparedFrame newFrame = new PreparedFrame(
	                    image,
	                    frameNumber,
	                    timestampUs,
	                    System.nanoTime());
	            
	            // Wait until we successfully offer the new frame
	            while (running) {
	            	if (readyFrame.offer(newFrame, 50, TimeUnit.MILLISECONDS)) { break; }
	            }
	            
			}
		} catch (InterruptedException e) {
			// stop() was called, exit quietly
		} catch (Exception e) {
			// OTHER Exceptions
			failure = e;
		}
	}
	
	// Helpers (from VideoPlayerModel)
	private Frame grabFrame() throws Exception {
        if (audioAvailable) {
            return grabber.grab();
        }
        return grabber.grabImage();
    }
	
	private void initialiseFrameProcessors(InfoVideo videoInfo) throws Exception {
        for (FrameProcessor processor : frameProcessors) {
            processor.initialise(videoInfo);
        }
    }
	
	private void processFrame(Frame frame, InfoFrame info) throws Exception {
        for (FrameProcessor processor : frameProcessors) {
            processor.process(frame, info);
        }
    }
}
