package comp3011;

import org.bytedeco.javacv.Frame;

public record FrameJob(Frame frame, int frameNumber, long mediaTimestampUs) {
    static final FrameJob END = new FrameJob(null, -1, -1);
}