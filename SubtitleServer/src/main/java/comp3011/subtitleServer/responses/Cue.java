package comp3011.subtitleServer.responses;

import java.util.List;

// The record holding each snippet of the subtitles content
public record Cue(long startUs, long endUs, List<String> lines) {}