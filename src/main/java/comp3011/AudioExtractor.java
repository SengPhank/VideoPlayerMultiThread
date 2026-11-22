package comp3011;

import java.io.File;
import java.nio.file.Files;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

/*
 * Extracts time-bounded audio chunk from a media file as compressed MP3 bytes. 
 * Uses FFmpegFrameGrabber to slice mp3 section.
*/
public class AudioExtractor {
	// Hide non-critical console logs
	static { avutil.av_log_set_level(avutil.AV_LOG_ERROR); }
	
    public static byte[] extractMp3Chunk(File videoFile, long startUs, long durationUs) throws Exception {
        File tempMp3 = File.createTempFile("subtitles-chunk-", ".mp3");
        long endUs = startUs + durationUs;
        boolean recordedAny = false;

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoFile)) {
        	grabber.start();
        	if (!grabber.hasAudio()) {
                return new byte[0];
            }

            long totalUs = grabber.getLengthInTime();
            if (totalUs > 0 && startUs >= totalUs) {
                return new byte[0];
            }

            if (startUs > 0) {
                grabber.setTimestamp(startUs);
            }

            try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(tempMp3, grabber.getAudioChannels())) {
                recorder.setFormat("mp3");
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_MP3);
                recorder.setSampleRate(grabber.getSampleRate());
                recorder.setAudioBitrate(64_000);
                recorder.start();

                Frame frame;
                while ((frame = grabber.grabSamples()) != null) {
                    long ts = grabber.getTimestamp();
                    if (ts < startUs) {
                        continue; // skip samples before the chunk window (keyframe seek landing)
                    }
                    if (ts >= endUs) {
                        break;
                    }
                    recorder.record(frame);
                    recordedAny = true;
                }
                recorder.stop();
            }
            grabber.stop();

            return recordedAny ? Files.readAllBytes(tempMp3.toPath()) : new byte[0];
        } finally {
            Files.deleteIfExists(tempMp3.toPath());
        }
    }
}