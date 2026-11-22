package comp3011.subtitleServer.controller;

import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import comp3011.subtitleServer.responses.ErrorResponse;
import comp3011.subtitleServer.responses.ShutdownResponse;
import comp3011.subtitleServer.responses.UptimeResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Instant;

/*
 * Admin Controller: Handles endpoints for:
 * Shutting down server
 * Getting uptime
 */
@RestController
public class AdminController {
		
	// When did this server start running?
	private final Instant serverStartUTC = Instant.now();
	private final long serverStartMilli = System.currentTimeMillis();
	
	// Is the application shutting down yet
	private final java.util.concurrent.atomic.AtomicBoolean isShuttingDown = new java.util.concurrent.atomic.AtomicBoolean(false);
	
	// Store the application container (so we can delete/shutdown later)
	private ApplicationContext context;
	public AdminController(ApplicationContext context) {
		this.context = context;
	}
	
    @GetMapping("/api/v1/admin/uptime")
    public UptimeResponse getServerUpTime( ) {
    	
    	long nowMilli = System.currentTimeMillis();
    	double timeElapsed = (nowMilli - serverStartMilli) / 1000.0;
        return new UptimeResponse(serverStartUTC, Instant.now(), timeElapsed);
    }
    
    @PostMapping("/api/v1/admin/shutdown")
    public ResponseEntity<?> reqShutdown(HttpServletRequest req) {
    	// Reject shutdown if already shutting down
    	if (!isShuttingDown.compareAndSet(false, true)) {
            ErrorResponse newErr = new ErrorResponse(
                Instant.now(),
                409,
                "Conflict",
                "Graceful shutdown is already in progress.",
                req.getRequestURI()
            );
            return ResponseEntity.status(409).body(newErr);
        }
    	
    	// Give time for client to receive shutdown response, then shutdown GRACEFULLY.
    	// We use and create a new Thread to allow this to happen; otherwise the code will immediately exit the server.
    	new Thread(() -> {
    		try {
    			Thread.sleep(500);
    		} catch (InterruptedException ignored) {}
    		// System.out.println("Shutting down...");
    		int exitCode = org.springframework.boot.SpringApplication.exit(context, () -> 0);
    		System.exit(exitCode);
    	}).start();
    	
    	ShutdownResponse newResp = new ShutdownResponse("Graceful shutdown requested.");
    	return ResponseEntity.status(202).body(newResp);
    }
}