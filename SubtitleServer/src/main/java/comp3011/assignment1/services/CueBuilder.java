package comp3011.assignment1.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import comp3011.assignment1.responses.Cue;
import comp3011.assignment1.responses.WhisperResponse;

/* Subtitle cue software requirements 
 * 
	The subtitle text should be legible regardless of the frame background.
	Subtitle text should be rendered using a san serif white font with a black outline.
	Subtitle text should be legible regardless of the frame background and disrupt the playing video scene as little as possible.
	Subtitle text should not exceed 42 characters per line.
	Subtitle text should not exceed two lines. Prefer one line when possible.
	Subtitle text for an entire phrase or sentence should appear on screen at the moment it is spoken, remaining there for no less than 1 second and no more than 7 seconds
	Subtitle text should be synchronised with dialogue at phrase and sentence level where possible - ie. bring up new subtitles following natural pauses in the dialogue where the timing permits.
	Line breaks, if required, should occur after punctuation where possible.
*/
@Service
public class CueBuilder {
    private static final int MAX_CHARS_PER_LINE = 42;
    private static final int MAX_LINES = 2;

    public List<Cue> build(WhisperResponse response) {
        if (response == null || response.segments().isEmpty()) {
            return List.of();
        }

        List<Cue> cues = new ArrayList<>();
        for (WhisperResponse.Segment seg : response.segments()) {
            // Spec: ignore segments with noSpeechProb > 0.6 and avgLogProb < -1.0
            if (seg.noSpeechProb() > 0.6 && seg.avgLogProb() < -1.0) {
                continue;
            }

            String text = seg.text() == null ? "" : seg.text().trim();
            if (text.isEmpty()) { continue; }

            long startUs = Math.round(seg.start() * 1_000_000.0);
            long endUs = Math.round(seg.end() * 1_000_000.0);
            List<String> lines = wrapLines(text);

            cues.add(new Cue(startUs, endUs, lines));
        }
        return cues;
    }

    /** Word wrapping adhering to the wants */
    private List<String> wrapLines(String text) {
        if (text.length() <= MAX_CHARS_PER_LINE) {
            return List.of(text);
        }

        List<String> lines = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            if (current.isEmpty()) {
                current.append(word);
            } else if (current.length() + 1 + word.length() <= MAX_CHARS_PER_LINE) {
                current.append(' ').append(word);
            } else {
                lines.add(current.toString());
                if (lines.size() == MAX_LINES) {
                    return lines;
                }
                current = new StringBuilder(word);
            }
        }

        if (!current.isEmpty() && lines.size() < MAX_LINES) {
            lines.add(current.toString());
        }
        return lines;
    }
}