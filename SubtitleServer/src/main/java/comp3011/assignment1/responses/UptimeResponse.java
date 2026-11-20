package comp3011.assignment1.responses;

import java.time.Instant;

public record UptimeResponse(
    Instant utcServerStart, // Server start encoded in RFC 3339
    Instant utcNow, // Now encoded in RFC 3339
    Double serverUptimeSeconds // How many seconds have elapsed since server start
) {};