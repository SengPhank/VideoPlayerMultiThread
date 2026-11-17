package comp3011;

import java.util.List;

public class CueBank {
	private final List<Cue> cueList;
	
	public CueBank(List<Cue> cues) {
		this.cueList = List.copyOf(cues);
	}
	
	// Returns the cue at time 
	public Cue cueTime(long timestampUs) {
		for (Cue c : cueList) {
			// If the time is within the cue, then return this cue
			if (timestampUs >= c.startUs() && timestampUs < c.endUs()) {
			    return c;
			}
		}
		return null;
	}
}