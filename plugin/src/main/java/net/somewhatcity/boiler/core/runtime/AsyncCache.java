package net.somewhatcity.boiler.core.runtime;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/** Bounded result cache with one outstanding request per key. */
public final class AsyncCache<K, V> implements AutoCloseable {
    private record Cached<V>(V value, long expires) {}
    private final Map<K, Cached<V>> values = new LinkedHashMap<>();
    private final Map<K, CompletableFuture<V>> pending = new HashMap<>();
    private final Executor executor;
    private final long ttl;
    private boolean closed;

    public AsyncCache(Executor executor, long ttlMillis) {
        this.executor = executor;
        ttl = TimeUnit.MILLISECONDS.toNanos(ttlMillis);
    }

    public synchronized CompletableFuture<V> get(K key, Supplier<V> loader) {
        if (closed) return CompletableFuture.failedFuture(new CancellationException("cache closed"));
        long now = System.nanoTime();
        values.values().removeIf(value -> value.expires() <= now);
        Cached<V> cached = values.get(key);
        if (cached != null) return CompletableFuture.completedFuture(cached.value());
        CompletableFuture<V> existing = pending.get(key);
        if (existing != null) return existing;
        CompletableFuture<V> request = new CompletableFuture<>();
        pending.put(key, request);
        try {
            executor.execute(() -> {
                try {
                    V value = loader.get();
                    synchronized (this) {
                        if (!closed) {
                            if (values.size() >= 256) values.remove(values.keySet().iterator().next());
                            values.put(key, new Cached<>(value, System.nanoTime() + ttl));
                        }
                    }
                    request.complete(value);
                } catch (Exception e) { request.completeExceptionally(e); }
                finally { synchronized (this) { pending.remove(key, request); } }
            });
        } catch (RejectedExecutionException e) {
            pending.remove(key);
            request.completeExceptionally(e);
        }
        return request;
    }

    @Override public synchronized void close() {
        closed = true;
        values.clear();
        for (CompletableFuture<V> future : pending.values()) future.cancel(false);
        pending.clear();
    }
}
