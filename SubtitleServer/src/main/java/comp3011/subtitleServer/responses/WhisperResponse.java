package comp3011.subtitleServer.responses;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Shape of whisper-1 "verbose_json" output with word + segment timestamps.
// Reference: https://platform.openai.com/docs/api-reference/audio/verbose-json-object
@JsonIgnoreProperties(ignoreUnknown = true)
public record WhisperResponse(
        String text,
        double duration,
        List<Segment> segments,
        List<Word> words) {

    public WhisperResponse {
        segments = segments == null ? List.of() : List.copyOf(segments);
        words = words == null ? List.of() : List.copyOf(words);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Segment(
            String text,
            double start,
            double end,
            @JsonProperty("avg_logprob") double avgLogProb,
            @JsonProperty("no_speech_prob") double noSpeechProb) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Word(String word, double start, double end) {}
}