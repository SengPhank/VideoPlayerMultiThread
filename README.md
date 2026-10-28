# COMP3011 Assignment 2 - Multi-Threaded Video Player

## Run

    mvn compile javafx:run "-Djavafx.args=-nsfwyvdjmbp path/to/video.mp4"
    mvn verify        # runs the unit tests

Run with `--help` for the effect flags. Effects are applied in command-line order and may be repeated.

## Threading architecture

    decoder -> q0 -> [stage 0] -> q1 -> [stage 1] -> ... -> qN -> converter -> readyFrames -> FX thread

| Thread | Class | Job |
| --- | --- | --- |
| frame-decoder | FrameDecoder | Reads packets from FFmpeg, queues audio, clones each video frame |
| frame-processor-0..K-1 | FrameProcessingStage | Applies its own slice of the effect list |
| frame-converter | FrameConversionStage | Converts the processed Frame to a JavaFX Image |
| JavaFX thread | VideoPlayerModel | Polls ready frames, runs the playback clock, writes audio, updates the GUI |

`FramePipeline` builds and owns the threads and queues. The effects list is
split into K contiguous groups, where `K = min(number of effects, CPU cores)`,
and each group gets its own thread.

## Why it is free of race conditions

- **One owner per effect.** Each FrameProcessor instance is only ever touched by one stage thread. Bleeder, Mottler and Jitterer mutate per-instance scratch buffers inside `process()`, so one instance could not safely run two frames at once.
- **Order is preserved.** Groups are contiguous slices of the command-line list, every queue is FIFO, and each stage has a single consumer. Frames cannot overtake each other, and effect 2 always sees effect 1's output.
- **Hand-off is safe.** Frames move between threads only through `ArrayBlockingQueue`, which gives a happens-before edge. A stage owns a frame exclusively until it passes it on, so there is no frame tearing. Processors are initialised on the decoder thread before the first job is queued, so that state is visible to the stage threads.
- **Back-pressure.** Queues are small and bounded (2 jobs, 4 ready frames), so a fast stage blocks instead of piling up native frames in memory.
- **Shutdown and seek.** `stop()` clears the running flag, interrupts every thread, joins them, then drains the queues and closes leftover native frames. Seeking stops the pipeline, repositions the grabber and restarts it, so no stale frames survive.
- **FX thread.** Only the JavaFX thread touches the playback clock and the view. Background threads only publish finished frames through the queue (`readyFrames`) and report errors through an `AtomicReference`.
- **Frame pacing.** `pumpPlayback` only fetches a new frame when it is not already holding one, and waits for a few frames to be buffered before starting the clock.

## Design trade-offs

- The throughput is limited by the slowest stage.
- Capping threads at the core count avoids oversubscribing on small machines.
- Running whole frames on a pool of workers. That would need every effect to be thread-safe, and the per-instance random seeds would differ between copies, breaking deterministic output.

## Results

| Vignette filters | Single-thread missed deadlines | Multi-thread missed deadlines |
| --- | --- | --- |
| 5 | 7/24 (29%) | 0/24 (0%) |
| 6 | 11/24 (46%) | 0/24 (0%) |
| 7 | 22/24 (92%) | 0/24 (0%) |
| | | |
| 46 | Untested | 0/24 (0%) | 
| 47 | Untested |16/24 (67%) |
| 48 | Untested | 24/24 (100%) |

Full logs and analysis can be found at [SingleCoreFindings](/docs/SingleThreadFindings.md) and [MultiThreadFindings](/docs/MultiThreadFindings.md)