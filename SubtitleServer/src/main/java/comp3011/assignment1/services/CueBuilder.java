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
    private static final int MAX_LINES_PER_CUE = 2;
    private static final long MIN_DURATION_US = 1_000_000L; // 1s
    private static final long MAX_DURATION_US = 7_000_000L; // 7s

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
            if (text.isEmpty()) {
                continue;
            }

            long segStartUs = Math.round(seg.start() * 1_000_000.0);
            long segEndUs = Math.round(seg.end() * 1_000_000.0);
            if (segEndUs <= segStartUs) {
                continue;
            }

            List<String> allLines = wrapAllLines(text);
            cues.addAll(splitIntoCues(allLines, segStartUs, segEndUs));
        }

        return clampDurations(cues);
    }

    // Wraps the entire text into lines of <= 42 chars
    private List<String> wrapAllLines(String text) {
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
                current = new StringBuilder(word);
            }

            // Spec: prefer breaking a line after sentence-ending punctuation
            if (endsWithBreakPunctuation(current.toString()) && current.length() >= 20) {
                lines.add(current.toString());
                current = new StringBuilder();
            }
        }

        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    private boolean endsWithBreakPunctuation(String s) {
        if (s.isEmpty()) return false;
        char last = s.charAt(s.length() - 1);
        return last == '.' || last == '?' || last == '!' || last == ';' || last == ',';
    }

    // Groups lines into 1- or 2-line cues, dividing the segment duration proportionally by length
    private List<Cue> splitIntoCues(List<String> allLines, long segStartUs, long segEndUs) {
        List<List<String>> groups = new ArrayList<>();
        for (int i = 0; i < allLines.size(); i += MAX_LINES_PER_CUE) {
            int end = Math.min(i + MAX_LINES_PER_CUE, allLines.size());
            groups.add(allLines.subList(i, end));
        }

        if (groups.size() == 1) {
            return List.of(new Cue(segStartUs, segEndUs, groups.get(0)));
        }

        int totalChars = groups.stream()
                .mapToInt(g -> g.stream().mapToInt(String::length).sum())
                .sum();
        long totalDurationUs = segEndUs - segStartUs;

        List<Cue> result = new ArrayList<>();
        long cursorUs = segStartUs;
        for (int i = 0; i < groups.size(); i++) {
            List<String> group = groups.get(i);
            long cueEndUs;
            if (i == groups.size() - 1) {
                cueEndUs = segEndUs;
            } else {
                int groupChars = group.stream().mapToInt(String::length).sum();
                long sliceUs = (totalChars > 0)
                        ? Math.round(totalDurationUs * ((double) groupChars / totalChars))
                        : totalDurationUs / groups.size();
                cueEndUs = Math.min(segEndUs, cursorUs + sliceUs);
            }
            result.add(new Cue(cursorUs, cueEndUs, group));
            cursorUs = cueEndUs;
        }
        return result;
    }

    /** Enforces the 1s min / 7s max guideline without overlapping the next cue. */
    private List<Cue> clampDurations(List<Cue> cues) {
        List<Cue> adjusted = new ArrayList<>(cues.size());
        for (int i = 0; i < cues.size(); i++) {
            Cue c = cues.get(i);
            long start = c.startUs();
            long end = c.endUs();
            long duration = end - start;

            if (duration > MAX_DURATION_US) {
                end = start + MAX_DURATION_US;
            } else if (duration < MIN_DURATION_US) {
                long desiredEnd = start + MIN_DURATION_US;
                if (i + 1 < cues.size()) {
                    desiredEnd = Math.min(desiredEnd, cues.get(i + 1).startUs());
                }
                end = Math.max(end, desiredEnd);
            }
            adjusted.add(new Cue(start, end, c.lines()));
        }
        return adjusted;
    }
}