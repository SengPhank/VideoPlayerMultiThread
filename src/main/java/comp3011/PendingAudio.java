package comp3011;

/*
 * Wraps a decoded chunk of PCM audio bytes (and its timestamp) waiting to be
 * written to AudioPlayer. Tracks a byte offset so AudioPlayer can write the
 * chunk across multiple non-blocking calls without holding playback back.
 */

public class PendingAudio {
    private final long timestampUs;
    private final byte[] samples;
    private int offset;

    PendingAudio(long timestampUs, byte[] samples) {
    	this.timestampUs = timestampUs;
    	this.samples = samples;
    }

    long timestampUs() {
        return timestampUs;
    }

    byte[] samples() {
        return samples;
    }

    int offset() {
        return offset;
    }

    int remaining() {
        return samples.length - offset;
    }

    void advance(int byteCount) {
        offset += byteCount;
    }

    boolean finished() {
        return offset >= samples.length;
    }
}