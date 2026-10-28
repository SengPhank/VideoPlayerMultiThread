// Authors:
//  1. Sengphachanh Phank a1967663
package comp3011;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.JavaFXFrameConverter;

import javafx.scene.image.Image;

final class FrameConversionStage implements Runnable {
	// Attributes ===
	
	// Thread stuff
	private final AtomicBoolean running;
	private final AtomicBoolean finished;
	private final AtomicReference<Exception> failure;
	
	private final BlockingQueue<FrameJob> processedJobs;
	private final BlockingQueue<PreparedFrame> readyFrames;

	private final JavaFXFrameConverter converter;
	
	// Constructor === 
	FrameConversionStage(BlockingQueue<FrameJob> processedJobs, BlockingQueue<PreparedFrame> readyFrames, JavaFXFrameConverter converter, AtomicBoolean running, AtomicReference<Exception> failure, AtomicBoolean finished) {
		this.processedJobs = processedJobs;
		this.readyFrames = readyFrames;
		this.converter = converter;
		this.running = running;
		this.failure = failure;
		this.finished = finished;
	}
	
	// Methods === 
			
	public void run() {
		try {
			while (running.get() == true) {
				
				// Take a job from the processed frames
				FrameJob frameJob = processedJobs.poll(50, TimeUnit.MILLISECONDS);
				
				if (frameJob == null) { continue; }
				
				// If end frame, then stop
				if (frameJob == FrameJob.END) {
					finished.set(true);
					return;
				}
				
				Frame frame = frameJob.frame();
				int frameNumber = frameJob.frameNumber();
				long timestampUs = frameJob.mediaTimestampUs();
				
				
				Image image;
				try {
				    image = converter.convert(frameJob.frame());
				} finally {
					// release the native copy
				    frameJob.frame().close();
				}
				
	            PreparedFrame preparedFrame = new PreparedFrame(
	            		image,
	            		frameNumber,
	            		timestampUs,
	            		System.nanoTime());
	            offerJob(preparedFrame);
			}
		} catch (InterruptedException e) {
			// stop() was called, exit quietly
		} catch (Exception e) {
			// OTHER Exceptions
			failure.set(e);
			running.set(false); // Stop on error
		}
	}
	
	// Offer the raw decoded frame to downstream workers
	private void offerJob(PreparedFrame readyJob) throws InterruptedException {
		while (running.get()) {
            if (readyFrames.offer(readyJob, 50, TimeUnit.MILLISECONDS)) { break; }
        }
    }
}