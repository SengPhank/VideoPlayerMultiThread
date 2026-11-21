package comp3011;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Talks to the local SubtitleServer to fetch subtitle cues for an audio track. */
public class SubtitleClient {
    private static final URI ENDPOINT = URI.create("http://localhost:8080/api/v1/subtitles");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public CueBank fetchCues(byte[] audioBytes, String filename) throws Exception {
        String boundary = "----Comp3011Boundary" + UUID.randomUUID();
        byte[] body = buildMultipartBody(boundary, audioBytes, filename);

        HttpRequest request = HttpRequest.newBuilder(ENDPOINT)
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Subtitle server returned HTTP " + response.statusCode() + ": " + response.body());
        }

        List<Cue> cues = MAPPER.readValue(response.body(), new TypeReference<List<Cue>>() {});
        return new CueBank(cues);
    }

    private byte[] buildMultipartBody(String boundary, byte[] fileBytes, String filename) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String header = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: audio/mpeg\r\n\r\n";
        out.write(header.getBytes(StandardCharsets.UTF_8));
        out.write(fileBytes);
        out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }
}