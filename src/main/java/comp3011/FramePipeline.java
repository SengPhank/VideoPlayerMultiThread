// Authors:
//  1. Sengphachanh Phank a1967663
package comp3011;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.JavaFXFrameConverter;

public class FramePipeline {
	// Attributes
	
	private static final int QUEUE_CAPACITY = 2;
    private static final int READY_CAPACITY = 4;
    
	private final AtomicBoolean running = new AtomicBoolean();
	private final AtomicBoolean finished = new AtomicBoolean();
	private final AtomicReference<Exception> failure = new AtomicReference<>();
	
	// raw/partly-processed frames (Frame + metadata)
	private final List<BlockingQueue<FrameJob>> jobQueues = new ArrayList<>();
	// finished Images for the UI
    private final BlockingQueue<PreparedFrame> readyFrames = new ArrayBlockingQueue<>(READY_CAPACITY);

    // decoder, processing stages…, converter (in order)
    private final List<Runnable> stages = new ArrayList<>();
    private final List<String> stageNames = new ArrayList<>();
    // live threads; replaced on every start()
    private List<Thread> threads = List.of();
	
	// Methods====
	
	// Takes the frame grabber, converter and processors from VideoPlayerModel
    FramePipeline(FFmpegFrameGrabber grabber, JavaFXFrameConverter converter, List<FrameProcessor> frameProcessors,
            String mediaName, boolean audioAvailable, AudioPlayer audioPlayer, Queue<PendingAudio> audioOut) {
    	
    	// Split the effects into K contiguous groups, one per processing thread.
        List<List<FrameProcessor>> groups = partition(frameProcessors, stageCountFor(frameProcessors.size()));

        // K processing stages need K + 1 queues. One in front of each stage, plus one between the last stage and the converter.
        for (int i = 0; i <= groups.size(); i++) {
            jobQueues.add(new ArrayBlockingQueue<>(QUEUE_CAPACITY));
        }
        
        // Stage 1 is the decoder. The only thread that touches the grabber, it writes to queue zero
        stages.add(new FrameDecoder(grabber, audioAvailable, jobQueues.get(0), audioOut, audioPlayer, mediaName,
                frameProcessors, running, failure));
        stageNames.add("frame-decoder");
        
        // Middle stages. Stage i reads queue i and writes queue i + 1, so frames pass through the groups in order.
        for (int i = 0; i < groups.size(); i++) {
            stages.add(new FrameProcessingStage(jobQueues.get(i), jobQueues.get(i + 1), groups.get(i), running, failure));
            stageNames.add("frame-processor-" + i);
        }
        
        // Last stage converts Frame to JavaFX Image and adds it to readyFrames. Reads the final job queue.
        stages.add(new FrameConversionStage(jobQueues.get(groups.size()), readyFrames, converter, running, failure,
                finished));
        stageNames.add("frame-converter");
    }
	
	public boolean isFinished() {
        return finished.get();
    }

	public Exception getFailure() {
        return failure.get();
    }
	
	// returns the next ready frame, or "null" if nothing is available
	public PreparedFrame pollFrame() {
		return readyFrames.poll();
	}
	
	// returns the number of frames ready for the UI
	public int readyCount() {
		return readyFrames.size();
	}
	// Start all (Decoder, processor, converter) threads
	public void start() {
        running.set(true);
        finished.set(false);
        failure.set(null);
 
        List<Thread> started = new ArrayList<>();
        for (int i = 0; i < stages.size(); i++) {
            Thread t = new Thread(stages.get(i), stageNames.get(i));
            t.setDaemon(true);
            started.add(t);
        }
        threads = List.copyOf(started);
        for (Thread t : threads) {
            t.start();
        }
    }
	
	// Stops all (Decoder, processor, converter) threads
	 public void stop() {
        running.set(false);
 
        // Interrupt everything first so all blocked queue operations wake at once, then join.
        for (Thread thread : threads) {
            thread.interrupt();
        }
        for (Thread thread : threads) {
            try {
                thread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Drain all queues on stopping
        readyFrames.clear();
        for (BlockingQueue<FrameJob> queue : jobQueues) {
            drain(queue);
        }
    }
	
	// Empties a queue and releases each frame's native memory. END has no frame inside it, hence the check.
	private static void drain(BlockingQueue<FrameJob> queue) {
	    FrameJob job;
	    while ((job = queue.poll()) != null) {
	        if (job != FrameJob.END) {
	            job.frame().close();
	        }
	    }
	}
	
	// Splits the list into 'groups' CONTIGUOUS slices (order must be preserved) whose sizes differ by at most one.
	// Contiguous because order matters. effect 2 needs effect 1's output on the same frame.
    static List<List<FrameProcessor>> partition(List<FrameProcessor> processors, int groups) {
        List<List<FrameProcessor>> result = new ArrayList<>();
        int base = processors.size() / groups;
        int extra = processors.size() % groups;
        int start = 0;
        for (int g = 0; g < groups; g++) {
            int size = base + (g < extra ? 1 : 0);
            // each slice is a read-only copy of the list
            result.add(List.copyOf(processors.subList(start, start + size)));
            start += size;
        }
        return result;
    }
    
    // How many processing threads to use. one per effect, but never more than the cores, and at least one
	static int stageCountFor(int processorCount) {
        return Math.max(1, Math.min(processorCount, Runtime.getRuntime().availableProcessors()));
    }
	
}
