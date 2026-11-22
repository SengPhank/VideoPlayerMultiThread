package comp3011.subtitleServer.services;

import java.util.concurrent.atomic.DoubleAdder;
import org.springframework.stereotype.Service;

// Running total of audio seconds sent to Whisper since server start. Threadsafe.
@Service
public class UsageService {
    private final DoubleAdder seconds = new DoubleAdder();

    public void addSeconds(double s) { seconds.add(s); }
    public double getSeconds() { return seconds.sum(); }
}