package comp3011;

import java.util.List;

public class CueBank {
	private final List<Cue> cueList;
	
	// TEST ONLY: hard-coded cues
	public static CueBank testBank() {
	    return new CueBank(List.of(
	        new Cue(1_000_000, 4_000_000, List.of("Welcome to SubtitleWise!", "This is a sample SRT subtitle.")),
	        new Cue(5_000_000, 9_000_000, List.of("You can use this file to test", "our subtitle tools.")),
	        new Cue(10_000_000, 14_000_000, List.of("Features include:", "Translation and editing"))));
	}
	
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