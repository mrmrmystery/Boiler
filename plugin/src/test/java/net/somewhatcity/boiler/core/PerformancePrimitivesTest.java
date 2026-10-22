package net.somewhatcity.boiler.core;

import net.somewhatcity.boiler.core.audio.PcmBuffer;
import net.somewhatcity.boiler.core.display.SourceGeneration;
import net.somewhatcity.boiler.core.runtime.AsyncCache;
import net.somewhatcity.boiler.core.runtime.AtomicFiles;
import net.somewhatcity.boiler.core.runtime.BoilerRuntime;
import net.somewhatcity.boiler.core.runtime.LatestTaskQueue;
import net.somewhatcity.boiler.core.sources.BgrxFrames;
import net.somewhatcity.boiler.core.sources.MediaQueue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class PerformancePrimitivesTest {
    @Test void pcmWrapsAndPadsWithSilence() throws Exception {
        PcmBuffer buffer = new PcmBuffer(4, false);
        buffer.write(new short[]{1, 2, 3});
        short[] first = new short[2];
        assertEquals(2, buffer.read(first));
        assertArrayEquals(new short[]{1, 2}, first);
        buffer.write(new short[]{4, 5, 6});
        short[] rest = new short[6];
        assertEquals(4, buffer.read(rest));
        assertArrayEquals(new short[]{3, 4, 5, 6, 0, 0}, rest);
    }

    @Test void pcmFileBackpressurePreservesEverySample() throws Exception {
        PcmBuffer buffer = new PcmBuffer(2, false);
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<?> producer = executor.submit(() -> {
                try { buffer.write(new short[]{1, 2, 3, 4}); }
                catch (InterruptedException e) { throw new AssertionError(e); }
            });
            await(() -> buffer.size() == 2);
            assertFalse(producer.isDone());
            short[] block = new short[2];
            buffer.read(block);
            assertArrayEquals(new short[]{1, 2}, block);
            producer.get(2, TimeUnit.SECONDS);
            buffer.read(block);
            assertArrayEquals(new short[]{3, 4}, block);
        }
    }

    @Test void pcmCloseReleasesBlockedProducerAndIsIdempotent() throws Exception {
        PcmBuffer buffer = new PcmBuffer(1, false);
        buffer.write(new short[]{1});
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<?> producer = executor.submit(() -> {
                try { buffer.write(new short[]{2}); }
                catch (InterruptedException e) { throw new AssertionError(e); }
            });
            buffer.close();
            buffer.close();
            producer.get(2, TimeUnit.SECONDS);
            assertEquals(0, buffer.size());
        }
    }

    @Test void livePcmRetainsNewestSamplesWithinCapacity() throws Exception {
        PcmBuffer buffer = new PcmBuffer(4, true);
        buffer.write(new short[]{1, 2, 3});
        buffer.write(new short[]{4, 5});
        short[] output = new short[4];
        buffer.read(output);
        assertArrayEquals(new short[]{2, 3, 4, 5}, output);
        buffer.write(new short[]{6, 7, 8, 9, 10, 11});
        buffer.read(output);
        assertArrayEquals(new short[]{8, 9, 10, 11}, output);
    }

    @Test void mediaFileQueueBlocksAndDrainsAtEof() throws Exception {
        MediaQueue<String> queue = new MediaQueue<>(1, false);
        queue.put("picture", 0);
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            CountDownLatch started = new CountDownLatch(1);
            Future<?> producer = executor.submit(() -> {
                started.countDown();
                try { queue.put("audio", 20_000); queue.finish(); }
                catch (InterruptedException e) { throw new AssertionError(e); }
            });
            assertTrue(started.await(2, TimeUnit.SECONDS));
            assertFalse(producer.isDone());
            assertEquals("picture", queue.take().value());
            producer.get(2, TimeUnit.SECONDS);
            assertEquals("audio", queue.take().value());
            assertNull(queue.take());
        }
    }

    @Test void liveMediaQueueDropsAudioAndPicturesOnOneTimeline() throws Exception {
        MediaQueue<String> queue = new MediaQueue<>(10, true);
        queue.put("picture0", 0);
        queue.put("audio0", 10_000);
        queue.put("picture1", 600_000);
        assertEquals(1, queue.size());
        var entry = queue.take();
        assertEquals("picture1", entry.value());
        assertTrue(entry.discontinuity());
        queue.put("audio1", 610_000);
        assertFalse(queue.take().discontinuity());
    }

    @Test void mediaCancelWakesEmptyConsumer() throws Exception {
        MediaQueue<String> queue = new MediaQueue<>(1, false);
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<?> consumer = executor.submit(() -> { try { assertNull(queue.take()); } catch (InterruptedException e) { throw new AssertionError(e); } });
            queue.cancel();
            queue.cancel();
            consumer.get(2, TimeUnit.SECONDS);
        }
    }

    @Test void mediaRecyclesOnlyDiscardedFramesIncludingCancellation() throws Exception {
        java.util.List<String> discarded = new java.util.ArrayList<>();
        MediaQueue<String> queue = new MediaQueue<>(1, true, discarded::add);
        queue.put("old", 0);
        queue.put("new", 10_000);
        assertEquals(java.util.List.of("old"), discarded);
        assertEquals("new", queue.take().value());
        queue.put("cancelled", 20_000);
        queue.cancel();
        queue.cancel();
        queue.put("after-close", 30_000);
        assertEquals(java.util.List.of("old", "cancelled", "after-close"), discarded);
    }

    @Test void bgrxBulkCopyPreservesColorsPaddingAndBufferPosition() {
        ByteBuffer bytes = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN);
        bytes.putInt(0x00112233).putInt(0x00445566).putInt(0x00ffffff);
        bytes.putInt(0x00778899).putInt(0x00aabbcc).putInt(0x00ffffff).flip();
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        BgrxFrames.copy(bytes, image, 12);
        assertEquals(0xff112233, image.getRGB(0, 0));
        assertEquals(0xff445566, image.getRGB(1, 0));
        assertEquals(0xff778899, image.getRGB(0, 1));
        assertEquals(0xffaabbcc, image.getRGB(1, 1));
        assertEquals(0, bytes.position());
        assertThrows(IllegalArgumentException.class, () -> BgrxFrames.copy(ByteBuffer.allocate(1), image, 8));
    }

    @Test void generationsRejectOldLoadsAndAllCallbacksAfterClose() {
        SourceGeneration generation = new SourceGeneration();
        long old = generation.next(), current = generation.next();
        assertFalse(generation.current(old));
        assertTrue(generation.current(current));
        generation.close();
        generation.close();
        assertFalse(generation.current(current));
        assertEquals(-1, generation.next());
    }

    @Test void asyncCacheCoalescesAndExpires() throws Exception {
        try (ExecutorService executor = Executors.newSingleThreadExecutor(); AsyncCache<String, Integer> cache = new AsyncCache<>(executor, 40)) {
            AtomicInteger calls = new AtomicInteger();
            CountDownLatch release = new CountDownLatch(1);
            var first = cache.get("stream", () -> {
                try { release.await(); } catch (InterruptedException e) { throw new RuntimeException(e); }
                return calls.incrementAndGet();
            });
            var second = cache.get("stream", calls::incrementAndGet);
            assertSame(first, second);
            release.countDown();
            assertEquals(1, first.get(2, TimeUnit.SECONDS));
            assertEquals(1, cache.get("stream", calls::incrementAndGet).get(2, TimeUnit.SECONDS));
            Thread.sleep(60);
            assertEquals(2, cache.get("stream", calls::incrementAndGet).get(2, TimeUnit.SECONDS));
        }
    }

    @Test void rejectedCacheRequestDoesNotStick() {
        try (AsyncCache<String, Integer> cache = new AsyncCache<>(task -> { throw new RejectedExecutionException(); }, 250)) {
            assertTrue(cache.get("stream", () -> 1).isCompletedExceptionally());
            assertTrue(cache.get("stream", () -> 2).isCompletedExceptionally());
        }
    }

    @Test void atomicSaveReplacesExistingFileWithoutTemporaryDebris(@TempDir Path directory) throws Exception {
        Path target = directory.resolve("displays/display_1.json");
        AtomicFiles.write(target, "old");
        AtomicFiles.write(target, "new");
        assertEquals("new", Files.readString(target));
        try (var files = Files.list(target.getParent())) { assertEquals(1, files.count()); }
    }

    @Test void runtimeStopsWorkersAndRejectsFurtherWork() throws Exception {
        BoilerRuntime runtime = new BoilerRuntime();
        Thread worker = runtime.startWorker("test", () -> {
            try { new CountDownLatch(1).await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        runtime.close();
        runtime.close();
        assertFalse(worker.isAlive());
        assertTrue(runtime.render().isShutdown());
        assertThrows(RejectedExecutionException.class, () -> runtime.startWorker("late", () -> {}));
    }

    @Test void sourceQueueCoalescesPendingLoadsAndCloseDiscardsPendingWork() throws Exception {
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            LatestTaskQueue queue = new LatestTaskQueue(executor);
            CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
            AtomicInteger last = new AtomicInteger();
            queue.submit(() -> {
                entered.countDown();
                try { release.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                last.set(1);
            });
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            for (int i = 2; i <= 10_000; i++) {
                int value = i;
                queue.submit(() -> { assertEquals(1, last.get()); last.set(value); });
            }
            release.countDown();
            await(() -> last.get() == 10_000);
            queue.close();
            queue.submit(() -> fail("Source task ran after removal"));
            assertEquals(10_000, last.get());
        }
    }

    static void await(java.util.function.BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) Thread.sleep(2);
        assertTrue(condition.getAsBoolean(), "Timed out waiting for asynchronous state");
    }
}
