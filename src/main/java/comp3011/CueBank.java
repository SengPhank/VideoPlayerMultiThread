package comp3011;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/* 
 * CueBank holds the local copy of the cues for its livespan
 * Stores all recorded cues in the list
 * If the cue already exists locally, we never have to reach the server
 * Only one unique mp4 per instance, so we never have to clear it.
 */
public class CueBank {
	// Uses a concurrent array list to make read more thread safe
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