// Authors:
//  1. Sengphachanh Phank a1967663
package comp3011;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;

final class FrameDecoder implements Runnable {
	// Attributes === 
	
	// Thread stuff
	private AtomicBoolean running;
	private AtomicReference<Exception> failure;
	private BlockingQueue<FrameJob> decodedJobs;
	
	private final Queue<PendingAudio> audioOut; // Decoded audio
	private final FFmpegFrameGrabber grabber;
	private final boolean audioAvailable;
	private final AudioPlayer audioPlayer;
	private final String mediaName;
	private final List<FrameProcessor> frameProcessors;
	private boolean frameProcessorsInitialised = false;
	
	// Constructor ===
	FrameDecoder(FFmpegFrameGrabber grabber, boolean audioAvailable, BlockingQueue<FrameJob> decodedJobs, Queue<PendingAudio> audioOut, AudioPlayer audioPlayer, String mediaName, List<FrameProcessor> frameProcessors, AtomicBoolean running, AtomicReference<Exception> failure) {
		this.grabber = grabber;
		this.audioAvailable = audioAvailable;
		this.decodedJobs = decodedJobs;
		this.audioOut = audioOut;
		this.audioPlayer = audioPlayer;
		this.mediaName = mediaName;
		this.frameProcessors = frameProcessors;
		this.running = running;
		this.failure = failure;
	}
	
	// Methods === 
		
	// run the thread pipeline, creating the filtered frames (with audio) and sending it to the video player.
	public void run() {
		try {
			while (running.get() == true) {
	            Frame frame = grabFrame();
	            
	            if (frame == null) {
	            	offerJob(FrameJob.END);
	                return;
	            }
	            
	            long timestampUs = grabber.getTimestamp();
	            
	            // Add audio to queue, if there is audio, convert and add it to the queue as well.
	            if (audioAvailable && frame.samples != null) {
	            	byte[] samples = audioPlayer.copySamples(frame);
		            if (samples.length > 0) {
		                audioOut.add(new PendingAudio(timestampUs, samples));
		            }
		        }
	            
	            // Loop again if no images
	            if (frame.image == null) {
		            continue;
	            }
	            
	            // On the first video frame, initialise the processors.
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
	            frame = frame.clone();
	            FrameJob job = new FrameJob(frame, frameNumber, timestampUs);
	            
	            // Send the decoded frame down the pipeline
	            offerJob(job);
                
	            
			}
		} catch (InterruptedException e) {
			// stop() was called, exit quietly
		} catch (Exception e) {
			// OTHER Exceptions
			failure.set(e);
			running.set(false);
		}
	}
	
	// Offer the raw decoded frame to downstream workers
	private void offerJob(FrameJob job) throws InterruptedException {
		while (running.get()) {
            if (decodedJobs.offer(job, 50, TimeUnit.MILLISECONDS)) { break; }
        }
    }
	
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
}