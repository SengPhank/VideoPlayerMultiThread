package comp3011.subtitleServer.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import comp3011.subtitleServer.services.UsageService;

/*
 * GlobalController handles one endpoint:
 * Usage checking, to see how much audio seconds were transcribed
 */
@RestController
public class GlobalController {
    private final UsageService usageService;

    public GlobalController(UsageService usageService) { this.usageService = usageService; }

    // Brief requires exactly { "usage": s } with s in seconds
    @GetMapping("/api/v1/global/usage")
    public Map<String, Double> getUsage() {
        return Map.of("usage", usageService.getSeconds());
    }
}