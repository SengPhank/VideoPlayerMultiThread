// Authors:
//  1. Sengphachanh Phank a1967663
package comp3011;

import org.bytedeco.javacv.Frame;

// Record of each job for frame processor
public record FrameJob(Frame frame, int frameNumber, long mediaTimestampUs) {
	// Poison pill to shutdown work
    static final FrameJob END = new FrameJob(null, -1, -1);
}