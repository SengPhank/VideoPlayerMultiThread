package comp3011.assignment1.services;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import comp3011.assignment1.responses.WhisperResponse;

/** The only class that talks to OpenAI, so it is the only place usage is counted. */
@Service
public class TranscriptionService {
    private static final String ENDPOINT = "https://api.openai.com/v1/audio/transcriptions";

    private final String apiKey;
    private final UsageService usageService;
    private final RestClient restClient;

    public TranscriptionService(@Value("${openai.api.key}") String apiKey, UsageService usageService) {
        this.apiKey = apiKey;
        this.usageService = usageService;

        // Without timeouts a dead network would hold a request thread forever.
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(60));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    /** Sends one audio chunk (e.g. WAV bytes) to whisper-1 and counts its duration. */
    public WhisperResponse transcribe(byte[] audio, String filename) {
        ByteArrayResource file = new ByteArrayResource(audio) {
            @Override public String getFilename() { return filename; }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file);
        body.add("model", "whisper-1");
        body.add("response_format", "verbose_json");
        body.add("timestamp_granularities[]", "word");
        body.add("timestamp_granularities[]", "segment");

        WhisperResponse resp = restClient.post()
                .uri(ENDPOINT)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(WhisperResponse.class);

        if (resp != null) usageService.addSeconds(resp.duration());
        return resp;
    }
}