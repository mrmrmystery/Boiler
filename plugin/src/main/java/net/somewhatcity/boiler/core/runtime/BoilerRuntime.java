package net.somewhatcity.boiler.core.runtime;

import okhttp3.OkHttpClient;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** All plugin-owned background work has one lifetime, including native reader threads. */
public final class BoilerRuntime implements AutoCloseable {
    private final ScheduledThreadPoolExecutor render = scheduler("render", Math.min(8, Math.max(2, Runtime.getRuntime().availableProcessors())));
    private final ScheduledThreadPoolExecutor audio = scheduler("audio", 2);
    private final ExecutorService io = pool("io");
    private final ExecutorService loaders = pool("load");
    private final Set<Thread> workers = ConcurrentHashMap.newKeySet();
    private volatile boolean closed;
    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(5)).readTimeout(Duration.ofSeconds(10))
            .callTimeout(Duration.ofSeconds(15)).build();

    private static ThreadFactory factory(String purpose) {
        AtomicInteger ids = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, "Boiler-" + purpose + "-" + ids.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    private static ScheduledThreadPoolExecutor scheduler(String name, int threads) {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(threads, factory(name));
        executor.setRemoveOnCancelPolicy(true);
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        return executor;
    }

    private static ExecutorService pool(String name) {
        // Reject overload instead of running blocking I/O on the submitting server thread.
        return new ThreadPoolExecutor(4, 4, 0, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(128), factory(name), new ThreadPoolExecutor.AbortPolicy());
    }

    public ScheduledExecutorService render() { return render; }
    public ScheduledExecutorService audio() { return audio; }
    public ExecutorService io() { return io; }
    public ExecutorService loaders() { return loaders; }
    public OkHttpClient http() { return http; }

    public synchronized Thread startWorker(String name, Runnable action) {
        if (closed) throw new RejectedExecutionException("Boiler is stopping");
        Thread thread = new Thread(() -> {
            try { action.run(); }
            finally { workers.remove(Thread.currentThread()); }
        }, "Boiler-" + name);
        thread.setDaemon(true);
        workers.add(thread);
        thread.start();
        return thread;
    }

    @Override public void close() {
        synchronized (this) {
            if (closed) return;
            closed = true;
        }
        render.shutdownNow();
        audio.shutdownNow();
        loaders.shutdownNow();
        io.shutdownNow();
        workers.forEach(Thread::interrupt);
        http.dispatcher().cancelAll();
        http.dispatcher().executorService().shutdownNow();
        http.connectionPool().evictAll();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(6);
        try {
            for (Thread thread : Set.copyOf(workers)) {
                if (thread == Thread.currentThread()) continue;
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) break;
                thread.join(Math.max(1, TimeUnit.NANOSECONDS.toMillis(remaining)));
            }
            for (ExecutorService service : new ExecutorService[]{render, audio, loaders, io}) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) break;
                service.awaitTermination(remaining, TimeUnit.NANOSECONDS);
            }
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        if (workers.stream().anyMatch(Thread::isAlive) || !loaders.isTerminated() || !io.isTerminated())
            java.util.logging.Logger.getLogger("Boiler").warning("Some background work exceeded the six-second shutdown deadline");
    }
}
