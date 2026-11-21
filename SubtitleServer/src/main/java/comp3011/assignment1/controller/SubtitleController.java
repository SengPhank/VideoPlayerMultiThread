package comp3011.assignment1.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import comp3011.assignment1.responses.Cue;
import comp3011.assignment1.responses.WhisperResponse;
import comp3011.assignment1.services.CueBuilder;
import comp3011.assignment1.services.TranscriptionService;

@RestController
public class SubtitleController {
    private final TranscriptionService transcriptionService;
    private final CueBuilder cueBuilder;

    public SubtitleController(TranscriptionService transcriptionService, CueBuilder cueBuilder) {
        this.transcriptionService = transcriptionService;
        this.cueBuilder = cueBuilder;
    }

    @PostMapping(value = "/api/v1/subtitles", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<Cue> getSubtitles(@RequestParam("file") MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "audio.wav";
        WhisperResponse response = transcriptionService.transcribe(file.getBytes(), filename);
        return cueBuilder.build(response);
    }
}