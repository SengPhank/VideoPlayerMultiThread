package comp3011;


import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javafx.scene.image.Image;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.JavaFXFrameConverter;
import org.bytedeco.javacv.Frame;


public class FramePipeline {
	// Attributes
	private final AtomicBoolean running = new AtomicBoolean();
	private final AtomicBoolean finished = new AtomicBoolean();
	private final AtomicReference<Exception> failure = new AtomicReference<>();

	private final BlockingQueue<FrameJob> decodedJobs = new ArrayBlockingQueue<>(2);
	private final BlockingQueue<FrameJob> processedJobs = new ArrayBlockingQueue<>(2);
	private final BlockingQueue<PreparedFrame> readyFrames = new ArrayBlockingQueue<>(4);

	private final Runnable[] stages;
	private List<Thread> threads = List.of();
	
	// Methods====
	
	// Takes the frame grabber, converter and processors from VideoPlayerModel
	FramePipeline(FFmpegFrameGrabber grabber, JavaFXFrameConverter converter, List<FrameProcessor> frameProcessors, String mediaName, boolean audioAvailable, AudioPlayer audioPlayer, Queue<PendingAudio> audioOut) {
		
		FrameDecoder frameDecoder = new FrameDecoder(grabber, audioAvailable, decodedJobs, audioOut, audioPlayer, mediaName, frameProcessors, running, failure);
		FrameProcessingStage frameProcessingStage = new FrameProcessingStage(decodedJobs, processedJobs, frameProcessors, running, failure);
		FrameConversionStage frameConversionStage = new FrameConversionStage(processedJobs, readyFrames, converter, running, failure, finished);
		
		this.stages = new Runnable[] {
			frameDecoder,
			frameProcessingStage,
			frameConversionStage
        };
	}
	
	
	// Start all (Decoder, processor, converter) threads
	public void start() {
		running.set(true);;
		finished.set(false);
		failure.set(null);
		thread.setDaemon(true);
		thread.start();
	}
	
	// Stops all (Decoder, processor, converter) threads
	public void stop() {
		running.set(false);;
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
