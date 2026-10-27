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
	
	public boolean isFinished() {
        return finished.get();
    }

	public Exception getFailure() {
        return failure.get();
    }
	
	// Start all (Decoder, processor, converter) threads
	public void start() {
		running.set(true);;
		finished.set(false);
		failure.set(null);
		
		// Init threads
		Thread decoderThread = new Thread(stages[0]::run, "frame-decoder");
		Thread processorThread = new Thread(stages[1]::run, "frame-processor");
		Thread converterThread = new Thread(stages[2]::run, "frame-converter");
		
		threads = List.of(
			decoderThread,
			processorThread,
			converterThread
		);
		
		// start each thread as a background process
		for (Thread t : threads) {
			t.setDaemon(true);
			t.start();
		}
		
	}
	
	// Stops all (Decoder, processor, converter) threads
	public void stop() {
		running.set(false);;
		for (Thread thread : threads) {
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
		}
		readyFrames.clear();
	}
	
	// returns the next ready frame, or "null" if nothing is available
	public PreparedFrame pollFrame() {
		return readyFrames.poll();
	}
	
}
