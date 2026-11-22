package comp3011;

import java.util.concurrent.atomic.AtomicBoolean;

import org.bytedeco.javacv.Frame;

/* 
 * Wrapper extension class for all FrameProcessor to make all
 * FrameProcessor become active/non-active during runtime.
 * This allows for subtitles to be toggled during video playback
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