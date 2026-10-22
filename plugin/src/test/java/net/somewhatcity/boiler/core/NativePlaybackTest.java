package net.somewhatcity.boiler.core;

import com.google.gson.JsonObject;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.core.audio.BAudioPlayer;
import net.somewhatcity.boiler.core.runtime.BoilerRuntime;
import net.somewhatcity.boiler.core.sources.BufferedFFMPEGSource;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.ShortBuffer;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NativePlaybackTest {
    @Test void videoWithoutAudioIsPacedAndReachesLastFrame(@TempDir Path directory) throws Exception {
        avutil.av_log_set_level(avutil.AV_LOG_ERROR);
        Path video = directory.resolve("video.mp4");
        createVideo(video, 4);
        withPlugin((plugin, runtime) -> {
            IBoilerDisplay display = display();
            CountDownLatch ended = new CountDownLatch(1);
            doAnswer(invocation -> { ended.countDown(); return null; }).when(display).source(eq("error"), any());
            {
                BAudioPlayer audio = mock(BAudioPlayer.class);
                BufferedFFMPEGSource source = new TestSource(audio);
                long start = System.nanoTime();
                try {
                    source.load(display, data(video));
                    assertTrue(ended.await(4, TimeUnit.SECONDS), "Video-only playback never reached EOF");
                    assertTrue(System.nanoTime() - start >= TimeUnit.MILLISECONDS.toNanos(200), "Playback ignored media timestamps");
                    BufferedImage output = new BufferedImage(32, 16, BufferedImage.TYPE_INT_RGB);
                    Graphics2D graphics = output.createGraphics();
                    try { source.draw(graphics, new Rectangle(0, 0, 32, 16)); }
                    finally { graphics.dispose(); }
                    Color pixel = new Color(output.getRGB(16, 8));
                    assertTrue(pixel.getBlue() > 200 && pixel.getRed() < 30, "Final decoded frame was not displayed");
                    verify(audio, never()).play(any());
                    assertEquals(0, ((TestSource) source).audioCreations, "Video-only source created an audio encoder");
                } finally { source.unload(); }
            }
        });
    }

    @Test void nativeAudioIsConvertedTo48KhzMonoLittleEndian(@TempDir Path directory) throws Exception {
        avutil.av_log_set_level(avutil.AV_LOG_ERROR);
        Path audioFile = directory.resolve("audio.wav");
        try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(audioFile.toString(), 2)) {
            recorder.setFormat("wav");
            recorder.setAudioCodec(avcodec.AV_CODEC_ID_PCM_S16LE);
            recorder.setSampleRate(44100);
            recorder.start();
            short[] samples = new short[8820 * 2];
            java.util.Arrays.fill(samples, (short) 4000);
            recorder.recordSamples(44100, 2, ShortBuffer.wrap(samples));
        }
        withPlugin((plugin, runtime) -> {
            IBoilerDisplay display = display();
            CountDownLatch ended = new CountDownLatch(1);
            doAnswer(invocation -> { ended.countDown(); return null; }).when(display).source(eq("error"), any());
            {
                BAudioPlayer audio = mock(BAudioPlayer.class);
                BufferedFFMPEGSource source = new TestSource(audio);
                try {
                    source.load(display, data(audioFile));
                    assertTrue(ended.await(4, TimeUnit.SECONDS));
                    var chunks = org.mockito.ArgumentCaptor.forClass(byte[].class);
                    verify(audio, atLeastOnce()).play(chunks.capture());
                    int total = chunks.getAllValues().stream().mapToInt(bytes -> bytes.length).sum();
                    assertTrue(total > 18000 && total <= 19200, "Unexpected resampled byte count: " + total);
                    for (byte[] bytes : chunks.getAllValues()) {
                        assertEquals(0, bytes.length % 2);
                        short sample = (short) ((bytes[0] & 255) | (bytes[1] & 255) << 8);
                        assertTrue(sample > 3000 && sample < 6500, "PCM byte order or mono conversion is incorrect");
                    }
                } finally { source.unload(); }
            }
        });
    }

    @Test void repeatedNativeCancellationReleasesDecodeAndPlaybackThreads(@TempDir Path directory) throws Exception {
        avutil.av_log_set_level(avutil.AV_LOG_ERROR);
        Path video = directory.resolve("long.mp4");
        createVideo(video, 30);
        withPlugin((plugin, runtime) -> {
            {
                BAudioPlayer audio = mock(BAudioPlayer.class);
                for (int i = 0; i < 10; i++) {
                    BufferedFFMPEGSource source = new TestSource(audio);
                    source.load(display(), data(video));
                    Thread.sleep(15);
                    source.unload();
                    source.unload();
                    PerformancePrimitivesTest.await(() -> Thread.getAllStackTraces().keySet().stream()
                            .noneMatch(thread -> thread.isAlive() && (thread.getName().equals("Boiler-decode-99") || thread.getName().equals("Boiler-playback-99"))));
                }
            }
        });
    }

    @Test void loopingKeepsTheFinalFrameForItsFrameDuration(@TempDir Path directory) throws Exception {
        avutil.av_log_set_level(avutil.AV_LOG_ERROR);
        Path video = directory.resolve("loop.mp4");
        createVideo(video, 4);
        withPlugin((plugin, runtime) -> {
            IBoilerDisplay display = display();
            BufferedFFMPEGSource source = new TestSource(mock(BAudioPlayer.class));
            JsonObject data = data(video);
            data.addProperty("loop", true);
            BufferedImage output = new BufferedImage(32, 16, BufferedImage.TYPE_INT_RGB);
            try {
                source.load(display, data);
                PerformancePrimitivesTest.await(() -> {
                    Graphics2D graphics = output.createGraphics();
                    try { source.draw(graphics, new Rectangle(0, 0, 32, 16)); }
                    finally { graphics.dispose(); }
                    return new Color(output.getRGB(16, 8)).getBlue() > 200;
                });
                Thread.sleep(35);
                Graphics2D graphics = output.createGraphics();
                try { source.draw(graphics, new Rectangle(0, 0, 32, 16)); }
                finally { graphics.dispose(); }
                assertTrue(new Color(output.getRGB(16, 8)).getBlue() > 200, "Loop restarted before the final 100 ms frame finished");
                verify(display, never()).source(anyString(), any());
            } finally { source.unload(); }
        });
    }

    static class TestSource extends BufferedFFMPEGSource {
        private final BAudioPlayer audio;
        volatile int audioCreations;
        TestSource(BAudioPlayer audio) { this.audio = audio; }
        @Override protected BAudioPlayer createAudio(IBoilerDisplay display, boolean live) { audioCreations++; return audio; }
    }

    private static JsonObject data(Path file) {
        JsonObject data = new JsonObject();
        data.addProperty("url", file.toString());
        data.addProperty("buffer", 1);
        return data;
    }
    private static IBoilerDisplay display() {
        IBoilerDisplay display = mock(IBoilerDisplay.class);
        when(display.id()).thenReturn(99);
        when(display.width()).thenReturn(32);
        when(display.height()).thenReturn(16);
        return display;
    }
    private static void createVideo(Path target, int frames) throws Exception {
        try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(target.toString(), 32, 16, 0);
             Java2DFrameConverter converter = new Java2DFrameConverter()) {
            recorder.setFormat("mp4");
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_MPEG4);
            recorder.setFrameRate(10);
            recorder.start();
            BufferedImage image = new BufferedImage(32, 16, BufferedImage.TYPE_3BYTE_BGR);
            for (int i = 0; i < frames; i++) {
                Graphics2D graphics = image.createGraphics();
                try {
                    graphics.setColor(i == frames - 1 ? Color.BLUE : Color.RED);
                    graphics.fillRect(0, 0, 32, 16);
                } finally { graphics.dispose(); }
                recorder.record(converter.convert(image));
            }
        }
    }
    @FunctionalInterface interface Check { void run(BoilerPlugin plugin, BoilerRuntime runtime) throws Exception; }
    private void withPlugin(Check check) throws Exception {
        BoilerPlugin previous = BoilerPlugin.getPlugin();
        Field singleton = BoilerPlugin.class.getDeclaredField("plugin");
        singleton.setAccessible(true);
        try (BoilerRuntime runtime = new BoilerRuntime()) {
            BoilerPlugin plugin = mock(BoilerPlugin.class);
            singleton.set(null, plugin);
            when(plugin.runtime()).thenReturn(runtime);
            when(plugin.getLogger()).thenReturn(Logger.getLogger("BoilerNativeTest"));
            when(plugin.callOnMain(any())).thenAnswer(invocation -> ((Callable<?>) invocation.getArgument(0)).call());
            check.run(plugin, runtime);
        } finally { singleton.set(null, previous); }
    }
}
