package comp3011;

import java.awt.Font;

// Subtitle Fonts class

public final class SubtitleFont {
	// any installed font like "Arial", "Verdana", "Georgia"
    public static final String NAME = Font.SANS_SERIF;  
    public static final boolean BOLD = true;
    // font height as a fraction of the video height
    public static final double SIZE = 1.0 / 18;
    // outline thickness as a fraction of the font size (0 = none)
    public static final double OUTLINE = 1.0 / 7;

    private SubtitleFont() {}

    public static Font create(int videoHeight) {
        return new Font(NAME, BOLD ? Font.BOLD : Font.PLAIN, Math.max(12, (int) (videoHeight * SIZE)));
    }
}