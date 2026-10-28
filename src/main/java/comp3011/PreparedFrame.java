// Authors:
//  1. Sengphachanh Phank a1967663
package comp3011;

import javafx.scene.image.Image;

public record PreparedFrame(
        Image image,
        int frameNumber,
        long mediaTimestampUs,
        long preparedAtNs) {
}