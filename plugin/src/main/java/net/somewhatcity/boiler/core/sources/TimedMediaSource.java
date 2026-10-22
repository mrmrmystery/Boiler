package net.somewhatcity.boiler.core.sources;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.somewhatcity.boiler.api.IBoilerSource;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.core.BoilerPlugin;
import net.somewhatcity.boiler.core.audio.BAudioPlayer;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;

/** Dedicated native readers never occupy the finite source-loading or HTTP pools. */
abstract class TimedMediaSource implements IBoilerSource {
    private record Media(BufferedImage image, byte[] audio) {}
    private volatile boolean running;
    private volatile Thread decoder;
    private volatile Thread playback;
    private volatile BAudioPlayer audio;
    private final Object audioLock = new Object();
    private MediaQueue<Media> queue;
    private final Object imageLock = new Object();
    private BufferedImage image;
    private final java.util.concurrent.ConcurrentLinkedQueue<BufferedImage> reusableImages = new java.util.concurrent.ConcurrentLinkedQueue<>();

    protected boolean liveByDefault() { return false; }
    protected int defaultBuffer() { return 10; }
    protected void configure(FFmpegFrameGrabber grabber) {}
    protected BAudioPlayer createAudio(IBoilerDisplay display, boolean live) {
        return BoilerPlugin.getPlugin().callOnMain(() -> new BAudioPlayer(display, live));
    }

    @Override public void load(IBoilerDisplay display, JsonObject data) {
        boolean live = data.has("live") ? data.get("live").getAsBoolean() : liveByDefault();
        queue = new MediaQueue<>(data.has("buffer") ? data.get("buffer").getAsInt() : defaultBuffer(), live,
                media -> recycle(media.image()));
        running = true;
        decoder = BoilerPlugin.getPlugin().runtime().startWorker("decode-" + display.id(), () -> decode(display, data, live));
        playback = BoilerPlugin.getPlugin().runtime().startWorker("playback-" + display.id(), () -> play(display));
    }

    private void decode(IBoilerDisplay display, JsonObject data, boolean live) {
        boolean loop = data.has("loop") && data.get("loop").getAsBoolean();
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(data.get("url").getAsString());
             Java2DFrameConverter converter = new Java2DFrameConverter()) {
            grabber.setAudioChannels(1);
            grabber.setSampleRate(48000);
            grabber.setSampleFormat(avutil.AV_SAMPLE_FMT_S16);
            grabber.setPixelFormat(avutil.AV_PIX_FMT_BGR24);
            grabber.setTimeout(5000);
            grabber.setOption("rw_timeout", "5000000");
            configure(grabber);
            if (data.has("options")) for (JsonElement option : data.getAsJsonArray("options")) {
                JsonObject opt = option.getAsJsonObject();
                grabber.setOption(opt.get("key").getAsString(), opt.get("value").getAsString());
            }
            grabber.start();
            if (grabber.hasAudio() && running) {
                BAudioPlayer created = createAudio(display, live);
                synchronized (audioLock) {
                    if (running) audio = created;
                    else created.stop();
                }
            }
            long loopOffset = 0, lastTimestamp = 0, lastDuration = 20_000;
            boolean received = false;
            while (running && !Thread.currentThread().isInterrupted()) {
                Frame frame = grabber.grabFrame();
                if (frame == null) {
                    if (loop && received) {
                        loopOffset = Math.max(lastTimestamp + lastDuration, loopOffset + Math.max(0, grabber.getLengthInTime()));
                        received = false;
                        grabber.restart();
                        continue;
                    }
                    break;
                }
                received = true;
                long timestamp = Math.max(lastTimestamp, loopOffset + Math.max(0, frame.timestamp));
                lastTimestamp = timestamp;
                BufferedImage nextImage = null;
                byte[] samples = null;
                if (frame.image != null) {
                    double rate = grabber.getFrameRate();
                    lastDuration = Double.isFinite(rate) && rate > 0 ? Math.max(1, Math.round(1_000_000 / rate)) : 20_000;
                    // JavaCV 1.5.10 infers channels from padded row stride on narrow videos.
                    // BGR24 was explicitly requested, so padding must not become extra channels.
                    frame.imageChannels = 3;
                    BufferedImage converted = converter.getBufferedImage(frame);
                    // JavaCV reuses its converter image. Publish an owned, display-sized snapshot.
                    nextImage = reusableImages.poll();
                    if (nextImage == null || nextImage.getWidth() != display.width() || nextImage.getHeight() != display.height())
                        nextImage = new BufferedImage(display.width(), display.height(), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D graphics = nextImage.createGraphics();
                    try { graphics.drawImage(converted, 0, 0, nextImage.getWidth(), nextImage.getHeight(), null); }
                    finally { graphics.dispose(); }
                }
                if (frame.samples != null && frame.samples[0] instanceof ShortBuffer buffer) {
                    ShortBuffer input = buffer.duplicate();
                    ByteBuffer output = ByteBuffer.allocate(input.remaining() * 2).order(ByteOrder.LITTLE_ENDIAN);
                    output.asShortBuffer().put(input);
                    samples = output.array();
                    lastDuration = Math.max(1, samples.length / 2L * 1_000_000 / 48_000);
                }
                if (nextImage != null || samples != null) queue.put(new Media(nextImage, samples), timestamp);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            if (running) BoilerPlugin.getPlugin().getLogger().log(Level.WARNING, "Decoding display " + display.id(), e);
        } finally { queue.finish(); }
    }

    private void play(IBoilerDisplay display) {
        long origin = 0, firstTimestamp = 0;
        boolean started = false;
        try {
            MediaQueue.Entry<Media> entry;
            while (running && (entry = queue.take()) != null) {
                if (!started || entry.discontinuity()) {
                    origin = System.nanoTime();
                    firstTimestamp = entry.timestampMicros();
                    started = true;
                    BAudioPlayer player = audio;
                    if (entry.discontinuity() && player != null) player.clear();
                }
                long deadline = origin + (entry.timestampMicros() - firstTimestamp) * 1000;
                while (running) {
                    long remaining = deadline - System.nanoTime();
                    if (remaining <= 0) break;
                    LockSupport.parkNanos(Math.min(remaining, 20_000_000));
                    if (Thread.currentThread().isInterrupted()) return;
                }
                if (!running) return;
                Media media = entry.value();
                if (media.image() != null) synchronized (imageLock) {
                    BufferedImage previous = image;
                    image = media.image();
                    recycle(previous);
                }
                BAudioPlayer player = audio;
                if (media.audio() != null && player != null) player.play(media.audio());
            }
            // Preserve the final queued audio before moving to the existing end-of-content screen.
            while (running && audio != null && audio.getAudioQueueSize() > 0) {
                Thread.sleep(20);
            }
            if (running) {
                JsonObject error = new JsonObject();
                error.addProperty("message", "End of content");
                display.source("error", error);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            if (running) BoilerPlugin.getPlugin().getLogger().log(Level.WARNING, "Playing display " + display.id(), e);
        }
    }

    @Override public void unload() {
        running = false;
        if (queue != null) queue.cancel();
        if (decoder != null) decoder.interrupt();
        if (playback != null) playback.interrupt();
        synchronized (audioLock) { if (audio != null) audio.stop(); }
        synchronized (imageLock) { image = null; }
        synchronized (reusableImages) { reusableImages.clear(); }
    }

    private void recycle(BufferedImage previous) {
        synchronized (reusableImages) {
            if (running && previous != null) reusableImages.offer(previous);
        }
    }

    @Override public void draw(Graphics2D graphics, Rectangle viewport) {
        synchronized (imageLock) { graphics.drawImage(image, 0, 0, viewport.width, viewport.height, null); }
    }
}
