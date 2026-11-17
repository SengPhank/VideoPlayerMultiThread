package comp3011;

import java.nio.ByteBuffer;
import java.util.List;
import org.bytedeco.javacv.Frame;

/** Frame processor that bakes the current cue's text into each frame. How the text looks is SubtitleRenderer's job. */
public class FrameSubtitle extends FrameProcessor {  
    private volatile CueBank cueBank;
    // the cue that 'strip' was drawn for
    private Cue lastCue;
    // last cue's text, drawn once and reused for every frame it is on screen
    private SubtitleRenderer.Strip strip;
    
    private static int nextInstanceIndex;
    private final int instanceIndex;

    public FrameSubtitle() {
        super();
        instanceIndex = nextInstanceIndex++;
    }
    public void setCueBank(CueBank cueBank) {
        this.cueBank = cueBank;
    }

    @Override
    public void initialise(InfoVideo info) throws Exception {
        super.initialise(info);
        lastCue = null;
        strip = null;
        
        // The very first text drawing loads fonts and is slow, so do it now rather than in the middle of playback.
        if (videoInfo.imageWidth() > 0 && videoInfo.imageHeight() > 0) {
            SubtitleRenderer.render(new Cue(0, 0, List.of("warm-up")), videoInfo.imageWidth(), videoInfo.imageHeight());
        }
    }

    @Override
    public void process(Frame frame, InfoFrame info) throws Exception {
        if (frame.image == null || frame.image.length == 0 || frame.image[0] == null) {
            return;
        }

        if (!(frame.image[0] instanceof ByteBuffer pixels)) {
            throw new Exception("FrameSubtitle requires byte-backed image frames");
        }

        if (videoInfo.imageDepth() != Frame.DEPTH_UBYTE && videoInfo.imageDepth() != Frame.DEPTH_BYTE) {
            throw new Exception("FrameSubtitle requires 8-bit image frames");
        }

        if (videoInfo.imageChannels() < 1) {
            throw new Exception("FrameSubtitle requires at least one image channel");
        }

        if (videoInfo.imageWidth() <= 0 || videoInfo.imageHeight() <= 0) {
            return;
        }

        CueBank bank = cueBank;
        // no video opened yet, no cues
        if (bank == null) { return; }

        Cue cue = bank.cueTime(info.timestampUs());
        // no cue text, return
        if (cue == null) { return; }
        
        // reuse last cue strip
        if (cue != lastCue) {
            strip = SubtitleRenderer.render(cue, videoInfo.imageWidth(), videoInfo.imageHeight());
            lastCue = cue;
        }
        drawStrip(pixels);
    }


    // Goes over the bottom part of the frame and mixes in every pixel of the text picture that isn't transparent.
    private void drawStrip(ByteBuffer pixels) {
        for (int y = strip.top(); y < videoInfo.imageHeight(); y++) {
            for (int x = 0; x < videoInfo.imageWidth(); x++) {
                int argb = strip.argb()[(y - strip.top()) * videoInfo.imageWidth() + x];
                if ((argb >>> 24) != 0) {
                    blendPixel(pixels, x, y, argb);
                }
            }
        }
    }
    
    // Mixes one text pixel into the frame. Partly transparent pixels (the soft edges of letters) keep some video.
    private void blendPixel(ByteBuffer pixels, int x, int y, int argb) {
        int base = y * videoInfo.imageStride() + x * videoInfo.imageChannels();
        int alpha = argb >>> 24;

        for (int channel = 0; channel < Math.min(3, videoInfo.imageChannels()); channel++) {
            int text = (argb >> (8 * channel)) & 255;
            int video = Byte.toUnsignedInt(pixels.get(base + channel));
            pixels.put(base + channel, (byte) ((text * alpha + video * (255 - alpha)) / 255));
        }
    }
}