package comp3011.assignment1.responses;

import java.time.Instant;

public record ErrorResponse(
    Instant timestamp, // Now in UTC Time encoded in RFC 3339
    int status, // Error code (e.g 200, 404, 500, ...)
    String error, // Error code convention (Get it from HttpStatus Library)
    String message, // Human-readable error reason
    String path // Path-to-API-call
) {};