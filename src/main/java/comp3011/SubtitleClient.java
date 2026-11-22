package comp3011;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


/*
 * HTTP client responsible for communicating with SubtitleServer.
 * Grabs already extracted mp3 audio and uploads it to server via multipart/form-data
 * parses response back into list of cues
 */

/* 
 * AI Assistance was used to help build
 * multipart/form-data HTTP request and JSON response deserialisation
 */
public class SubtitleClient {
    private static final String BASE_URL = "http://localhost:8080/api/v1/subtitles";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public List<Cue> fetchCues(byte[] audioBytes, String videoName, long offsetUs) throws Exception {
        String encodedName = URLEncoder.encode(videoName, StandardCharsets.UTF_8);
        URI endpoint = URI.create(BASE_URL + "?videoName=" + encodedName + "&offsetUs=" + offsetUs);

        String boundary = "----Comp3011Boundary" + UUID.randomUUID();
        byte[] body = buildMultipartBody(boundary, audioBytes, "chunk.mp3");

        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Subtitle server returned HTTP " + response.statusCode() + ": " + response.body());
        }

        return MAPPER.readValue(response.body(), new TypeReference<List<Cue>>() {});
    }
    
    // Wraps the raw MP3 bytes in a multipart/form-data body so then Spring Boot can read it as a MultipartFile
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