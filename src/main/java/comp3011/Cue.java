package comp3011;

import java.util.List;

/* Record of each caption chunk */

public record Cue(
		long startUs, 
		long endUs, 
		List<String> lines) 
{}