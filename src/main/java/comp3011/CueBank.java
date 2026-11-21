package comp3011;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CueBank {
    private final List<Cue> cueList = new CopyOnWriteArrayList<>();

    public CueBank() {}

    public CueBank(List<Cue> cues) {
        this.cueList.addAll(cues);
    }

    public void addAll(List<Cue> cues) {
        if (cues != null && !cues.isEmpty()) {
            this.cueList.addAll(cues);
        }
    }

    // Returns the cue active at timestampUs, or null if none
    public Cue cueTime(long timestampUs) {
        for (Cue c : cueList) {
            if (timestampUs >= c.startUs() && timestampUs < c.endUs()) {
                return c;
            }
        }
        return null;
    }
}