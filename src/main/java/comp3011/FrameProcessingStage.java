package comp3011;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

final class FrameProcessingStage implements Runnable {
	
	// Attributes ===
	private final BlockingQueue<FrameJob> in; // Incoming jobs (from decoder)
    private final BlockingQueue<FrameJob> out; // Output 
    private final List<FrameProcessor> frameProcessors;
    private final AtomicBoolean running;
    private final AtomicReference<Exception> failure;
	
	// Constructor === 
	FrameProcessingStage(BlockingQueue<FrameJob> in, BlockingQueue<FrameJob> out, List<FrameProcessor> processors, AtomicBoolean running, AtomicReference<Exception> failure) {
		this.in = in;
		this.out = out;
		this.frameProcessors = processors;
        this.running = running;
        this.failure = failure;
	}
	
	// Methods === 
	
	@Override
    public void run() {
		try {
			while (running.get() == true) {
				// Take a job from the decoded frames
				FrameJob job = in.poll(50, TimeUnit.MILLISECONDS);
				
				// if nothing arrived yet, loop and recheck
				if (job == null) { continue; }
				
				// If this is not the final frame, then 
				if (job != FrameJob.END) {
					// Process the frame in order
		            InfoFrame info = new InfoFrame(job.frameNumber(), job.mediaTimestampUs());
		            for (FrameProcessor processor : frameProcessors) {
		            	processor.process(job.frame(), info);
		            }
				}	

	            // Reoffer it to the conversion stage
	            offerJob(job);
	            
			}
		} catch (InterruptedException e) {
			// stop() was called, exit quietly
		} catch (Exception e) {
			// OTHER Exceptions
			failure.set(e);
			running.set(false); // stop on error
		}
	}
	
	// Offer the raw decoded frame to downstream workers
	private void offerJob(FrameJob job) throws InterruptedException {
		while (running.get()) {
            if (out.offer(job, 50, TimeUnit.MILLISECONDS)) { break; }
        }
    }
			
}