package comp3011.subtitleServer.controller;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import comp3011.subtitleServer.responses.Cue;
import comp3011.subtitleServer.responses.WhisperResponse;
import comp3011.subtitleServer.services.CueBuilder;
import comp3011.subtitleServer.services.TranscriptionService;

/*
 * Subtitle controller is the main endpoint for getting subtitles. 
 * Since it has first contact with the client for subtitles,
 * It will also store the subtitle and the timestamp.
 */

@RestController
public class SubtitleController {
    private final TranscriptionService transcriptionService;
    private final CueBuilder cueBuilder;

    // Cache key is given by: "<videoName>:<offsetUs>" -> shifted cues for that chunk
    private final ConcurrentHashMap<String, List<Cue>> cache = new ConcurrentHashMap<>();

    public SubtitleController(TranscriptionService transcriptionService, CueBuilder cueBuilder) {
        this.transcriptionService = transcriptionService;
        this.cueBuilder = cueBuilder;
    }

    @PostMapping(value = "/api/v1/subtitles", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<Cue> getSubtitles(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "videoName", defaultValue = "unknown") String videoName,
            @RequestParam(value = "offsetUs", defaultValue = "0") long offsetUs) throws IOException {

        String cacheKey = videoName + ":" + offsetUs;
        List<Cue> cached = cache.get(cacheKey);
        if (cached != null) {
            System.out.println("Cache HIT: " + cacheKey);
            return cached;
        }

        System.out.println("Cache MISS: " + cacheKey);
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "audio.mp3";
        WhisperResponse response = transcriptionService.transcribe(file.getBytes(), filename);
        List<Cue> cues = cueBuilder.build(response, offsetUs);
        cache.put(cacheKey, cues);
        return cues;
    }
}