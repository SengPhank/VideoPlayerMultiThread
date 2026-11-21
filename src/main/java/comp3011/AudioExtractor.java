package comp3011;

import java.io.File;
import java.nio.file.Files;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

/** Extracts just the audio track from a media file as compressed MP3 bytes. */
public class AudioExtractor {

    public static byte[] extractMp3(File videoFile) throws Exception {
        File tempMp3 = File.createTempFile("subtitles-", ".mp3");
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoFile)) {
            grabber.start();
            if (!grabber.hasAudio()) {
                return new byte[0];
            }

            try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(tempMp3, grabber.getAudioChannels())) {
                recorder.setFormat("mp3");
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_MP3);
                recorder.setSampleRate(grabber.getSampleRate());
                recorder.setAudioBitrate(64_000);
                recorder.start();

                Frame frame;
                while ((frame = grabber.grabSamples()) != null) {
                    recorder.record(frame);
                }
                recorder.stop();
            }
            grabber.stop();
            return Files.readAllBytes(tempMp3.toPath());
        } finally {
            Files.deleteIfExists(tempMp3.toPath());
        }
    }
}