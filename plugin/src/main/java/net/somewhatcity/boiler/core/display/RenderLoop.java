package net.somewhatcity.boiler.core.display;

import java.util.concurrent.*;

/** Maintains the requested start cadence without catch-up bursts or concurrent callbacks. */
public final class RenderLoop implements AutoCloseable {
    private final ScheduledExecutorService executor;
    private final Runnable render;
    private ScheduledFuture<?> future;
    private long generation;
    private int period;
    private boolean enabled;
    private boolean closed;
    private final java.util.concurrent.locks.ReentrantLock running = new java.util.concurrent.locks.ReentrantLock();

    public RenderLoop(ScheduledExecutorService executor, Runnable render) {
        this.executor = executor;
        this.render = render;
    }

    public synchronized void update(boolean enable, int millis) {
        if (closed) return;
        millis = Math.max(1, millis);
        if (enabled == enable && period == millis) return;
        enabled = enable;
        period = millis;
        long token = ++generation;
        if (future != null) future.cancel(false);
        future = null;
        if (enable) future = executor.schedule(() -> run(token), 0, TimeUnit.NANOSECONDS);
    }

    private void run(long token) {
        synchronized (this) { if (closed || !enabled || generation != token) return; }
        long start = System.nanoTime();
        try {
            if (running.tryLock()) {
                try {
                    synchronized (this) { if (closed || !enabled || generation != token) return; }
                    render.run();
                } finally { running.unlock(); }
            }
        }
        finally {
            synchronized (this) {
                if (!closed && enabled && token == generation) {
                    long interval = TimeUnit.MILLISECONDS.toNanos(period);
                    long elapsed = System.nanoTime() - start;
                    long delay = interval - elapsed % interval;
                    future = executor.schedule(() -> run(token), delay, TimeUnit.NANOSECONDS);
                }
            }
        }
    }

    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        ++generation;
        if (future != null) future.cancel(false);
        future = null;
    }
}
