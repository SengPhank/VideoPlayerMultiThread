package comp3011;

import java.util.concurrent.atomic.AtomicBoolean;

import org.bytedeco.javacv.Frame;

/* Wrapper class that can be used for each of our frame processors, 
 * so then can be enabled/disabled in real-time.
 * Instead of having to restart the app player.
 */

public class ToggleableProcessor extends FrameProcessor {
	private final FrameProcessor processor;
	private final AtomicBoolean enabled;
	
	public ToggleableProcessor (FrameProcessor processor, boolean startEnabled) {
		this.processor = processor;
		this.enabled = new AtomicBoolean(startEnabled);
	}
	
	// Initialise toggleable and frame processor
	@Override
	public void initialise(InfoVideo videoInfo) throws Exception {
		super.initialise(videoInfo);
		processor.initialise(videoInfo);
	}
	
	// Process only if enabled
	@Override
	public void process(Frame frame, InfoFrame info) throws Exception {
		if (enabled.get()) processor.process(frame, info);
	}
	
	public void setEnabled(boolean on) {
	    enabled.set(on);
	}
	
	public boolean toggle() {
		boolean state = !enabled.get();
		enabled.set(state);
		return state;
	}
	
	public boolean isEnabled() {
		return enabled.get();
	}
	
	public FrameProcessor getProcessor() {
	    return processor;
	}
}