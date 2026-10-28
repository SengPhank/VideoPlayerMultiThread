// Authors:
//  1. Sengphachanh Phank a1967663
package comp3011;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import org.bytedeco.javacv.Frame;

// Check the rule the pipeline's correctness on effects keep command-line order after being split into per-thread groups, and no effect is lost or duplicated.
class FramePipelineTest {
    private static FrameProcessor noop() {
        return new FrameProcessor() {
            @Override
            public void process(Frame frame, InfoFrame info) { }
        };
    }

    @Test
    void partitionKeepsOrderAndEveryProcessorExactlyOnce() {
        List<FrameProcessor> all = new ArrayList<>();
        for (int i = 0; i < 11; i++) all.add(noop());

        for (int groups = 1; groups <= 11; groups++) {
            List<FrameProcessor> flattened = new ArrayList<>();
            FramePipeline.partition(all, groups).forEach(flattened::addAll);
            assertEquals(all.size(), flattened.size());
            for (int i = 0; i < all.size(); i++) assertSame(all.get(i), flattened.get(i));
        }
    }

    @Test
    void groupSizesDifferByAtMostOne() {
        List<FrameProcessor> all = new ArrayList<>();
        for (int i = 0; i < 8; i++) all.add(noop());
        var groups = FramePipeline.partition(all, 3); // 3,3,2
        assertEquals(3, groups.get(0).size());
        assertEquals(3, groups.get(1).size());
        assertEquals(2, groups.get(2).size());
    }

    @Test
    void noEffectsStillGivesOneStage() {
        assertEquals(1, FramePipeline.stageCountFor(0));
        assertEquals(1, FramePipeline.partition(List.of(), 1).size());
    }
}