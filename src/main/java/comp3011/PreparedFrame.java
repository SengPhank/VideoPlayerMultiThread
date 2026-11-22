// Authors:
//  1. Sengphachanh Phank a1967663
package comp3011;

import javafx.scene.image.Image;

// Record for the final ready to display frame
public record PreparedFrame(
        Image image,
        int frameNumber,
        long mediaTimestampUs,
        long preparedAtNs) {
}