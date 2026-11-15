package comp3011;

import java.nio.ByteBuffer;

import org.bytedeco.javacv.Frame;

public class FrameSubtitle extends FrameProcessor {
	private final CueBank cueBank;
	
	public FrameSubtitle(CueBank cueBank) {
		this.cueBank = cueBank;
	}
	
	// Bake subtitles into frame
	@Override
	public void process(Frame frame, InfoFrame info) throws Exception {
		if (frame.image == null || frame.image.length == 0 || frame.image[0] == null) { return; }
        if (!(frame.image[0] instanceof ByteBuffer pixels)) {
            throw new Exception("Can't generate subtitle: FrameSubtitle requires byte-backed image frames");
        }

        Cue cue = cueBank.cueTime(info.timestampUs());
        if (cue == null) { return; }

        drawBar(pixels);
    }
	
	// Draws the subtitle box
	private void drawBar(ByteBuffer pixels) {
        int w = videoInfo.imageWidth();
        int h = videoInfo.imageHeight();
        int ch = videoInfo.imageChannels();
        int stride = videoInfo.imageStride();

        for (int y = (int) (h * 0.85); y < (int) (h * 0.95); y++) {
            int row = y * stride;
            for (int i = row + (w / 5) * ch; i < row + (w * 4 / 5) * ch; i++) {
                pixels.put(i, (byte) ((pixels.get(i) & 0xFF) >> 1));
            }
        }
    }
	
}