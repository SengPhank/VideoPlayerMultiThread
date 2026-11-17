package comp3011;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.List;

/* User requirements
- The subtitle text should be legible regardless of the frame background.
- Subtitle text should be rendered using a san serif white font with a black outline.
- Subtitle text should be legible regardless of the frame background and disrupt the playing video scene as little as possible.
- Subtitle text should not exceed 42 characters per line.
- Subtitle text should not exceed two lines. Prefer one line when possible.
- Subtitle text for an entire phrase or sentence should appear on screen at the moment it is spoken, remaining there for no less than 1 second and no more than 7 seconds
- Subtitle text should be synchronised with dialogue at phrase and sentence level where possible - ie. bring up new subtitles following natural pauses in the dialogue where the timing permits.
- Line breaks, if required, should occur after punctuation where possible.
 */

/** Draws one cue's text into pixels. It knows nothing about frames: FrameSubtitle copies the result onto them. */
public final class SubtitleRenderer {
	// the text lives in the bottom 35% of the video
    private static final double STRIP_START = 0.65;
    // space under the last line, as a fraction of the video height
    private static final double BOTTOM_GAP = 1.0 / 16;
    // the widest line may use at most this much of the video width
    private static final double MAX_WIDTH = 0.92;

    // The drawn text as 0xAARRGGBB pixels, for the rows from 'top' down to the bottom of the video.
    // We will use bit manip to decode this
    public record Strip(int[] argb, int top) {}

    private SubtitleRenderer() {}

    public static Strip render(Cue cue, int videoW, int videoH) {
        int top = (int) (videoH * STRIP_START);
        BufferedImage img = new BufferedImage(videoW, videoH - top, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        FontRenderContext frc = g.getFontRenderContext();

        // reject empty text
        List<String> lines = cue.lines().stream().filter(l -> !l.isBlank()).toList();
        Font font = SubtitleFont.create(videoH);
        double widest = widest(lines, font, frc);
        double pad = font.getSize2D() * 0.4;
        // too wide for the video: shrink the text to fit
        if (widest + 2 * pad > videoW * MAX_WIDTH) {
            font = font.deriveFont((float) (font.getSize2D() * videoW * MAX_WIDTH / (widest + 2 * pad)));
            widest = widest(lines, font, frc);
            pad = font.getSize2D() * 0.4;
        }

        double lineH = font.getLineMetrics("Ag", frc).getHeight();
        double blockBottom = img.getHeight() - videoH * BOTTOM_GAP;
        double blockTop = blockBottom - lines.size() * lineH;

        if (!lines.isEmpty() && SubtitleColours.BACKGROUND.getAlpha() > 0) {
            g.setColor(SubtitleColours.BACKGROUND);
            g.fillRoundRect((int) ((videoW - widest) / 2 - pad), (int) (blockTop - pad / 2),
                    (int) (widest + 2 * pad), (int) (lines.size() * lineH + pad), (int) pad, (int) pad);
        }

        g.setStroke(new BasicStroke((float) (font.getSize2D() * SubtitleFont.OUTLINE), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < lines.size(); i++) {
            TextLayout text = new TextLayout(lines.get(i), font, frc);
            // centre this line
            double x = (videoW - text.getAdvance()) / 2;
            // baseline
            double y = blockTop + i * lineH + font.getLineMetrics("Ag", frc).getAscent();
            Shape shape = text.getOutline(AffineTransform.getTranslateInstance(x, y));
            // outline the text first, then draw letters on top of it
            if (SubtitleFont.OUTLINE > 0) {
                g.setColor(SubtitleColours.OUTLINE);
                g.draw(shape);
            }
            // Draw the letters on top
            g.setColor(SubtitleColours.TEXT);
            g.fill(shape);
        }
        g.dispose();
        return new Strip(img.getRGB(0, 0, videoW, img.getHeight(), null, 0, videoW), top);
    }

    private static double widest(List<String> lines, Font font, FontRenderContext frc) {
        double widest = 0;
        for (String line : lines) {
            widest = Math.max(widest, new TextLayout(line, font, frc).getAdvance());
        }
        return widest;
    }
}