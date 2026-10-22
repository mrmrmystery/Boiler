package net.somewhatcity.boiler.core;

import net.somewhatcity.boiler.core.display.RenderLoop;
import net.somewhatcity.boiler.core.runtime.BoilerRuntime;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.junit.jupiter.api.Assertions.*;

class RenderSchedulingTest {
    @Test void fiftyDisplaysShareAtMostEightRenderThreads() throws Exception {
        try (BoilerRuntime runtime = new BoilerRuntime()) {
            List<RenderLoop> loops = new ArrayList<>();
            Set<Thread> threads = ConcurrentHashMap.newKeySet();
            AtomicInteger calls = new AtomicInteger();
            for (int count : new int[]{1, 10, 50}) {
                calls.set(0);
                threads.clear();
                try {
                    for (int i = 0; i < count; i++) {
                        RenderLoop loop = new RenderLoop(runtime.render(), () -> {
                            threads.add(Thread.currentThread());
                            calls.incrementAndGet();
                        });
                        loops.add(loop);
                        loop.update(true, 5);
                    }
                    PerformancePrimitivesTest.await(() -> calls.get() >= count * 3);
                    assertTrue(threads.size() <= 8);
                } finally {
                    loops.forEach(RenderLoop::close);
                    loops.clear();
                }
            }
        }
    }

    @Test void periodChangesDuringBlockedDrawNeverOverlapAndCloseNeverReschedules() throws Exception {
        try (BoilerRuntime runtime = new BoilerRuntime()) {
            CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
            AtomicInteger active = new AtomicInteger(), calls = new AtomicInteger();
            AtomicBoolean overlap = new AtomicBoolean();
            RenderLoop loop = new RenderLoop(runtime.render(), () -> {
                if (active.incrementAndGet() != 1) overlap.set(true);
                try {
                    calls.incrementAndGet();
                    entered.countDown();
                    release.await();
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                finally { active.decrementAndGet(); }
            });
            loop.update(true, 1000);
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            loop.update(true, 1);
            Thread.sleep(20);
            assertEquals(1, calls.get());
            loop.close();
            loop.close();
            release.countDown();
            PerformancePrimitivesTest.await(() -> active.get() == 0);
            Thread.sleep(20);
            assertFalse(overlap.get());
            assertEquals(1, calls.get());
        }
    }
}
