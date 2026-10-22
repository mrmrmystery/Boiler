package net.somewhatcity.boiler.core.runtime;

import java.util.concurrent.Executor;

/** Serial execution with one replaceable pending task, rather than an unbounded future chain. */
public final class LatestTaskQueue implements AutoCloseable {
    private final Executor executor;
    private Runnable pending;
    private boolean scheduled;
    private boolean closed;

    public LatestTaskQueue(Executor executor) { this.executor = executor; }

    public synchronized void submit(Runnable task) {
        if (closed) return;
        pending = task;
        if (scheduled) return;
        scheduled = true;
        try { executor.execute(this::drain); }
        catch (RuntimeException e) { scheduled = false; pending = null; throw e; }
    }

    private void drain() {
        try {
            while (true) {
                Runnable task;
                synchronized (this) {
                    if (closed || pending == null) { scheduled = false; return; }
                    task = pending;
                    pending = null;
                }
                task.run();
            }
        } catch (RuntimeException | Error e) {
            synchronized (this) { scheduled = false; pending = null; }
            throw e;
        }
    }

    @Override public synchronized void close() { closed = true; pending = null; }
}
