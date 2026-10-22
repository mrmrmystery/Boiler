package net.somewhatcity.boiler.core.audio;

import java.util.Arrays;

/** Bounded, primitive mono PCM ring. All lengths are in samples, never bytes. */
public final class PcmBuffer {
    private final short[] samples;
    private final boolean live;
    private int head;
    private int size;
    private boolean closed;

    public PcmBuffer(int capacity, boolean live) {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be positive");
        samples = new short[capacity];
        this.live = live;
    }

    public synchronized void write(short[] data) throws InterruptedException {
        int offset = 0;
        if (live && data.length > samples.length) offset = data.length - samples.length;
        while (offset < data.length && !closed) {
            while (size == samples.length && !closed && !live) wait();
            if (closed) return;
            if (live && size + data.length - offset > samples.length) {
                int discard = Math.min(size, size + data.length - offset - samples.length);
                head = (head + discard) % samples.length;
                size -= discard;
            }
            int tail = (head + size) % samples.length;
            int count = Math.min(data.length - offset, Math.min(samples.length - size, samples.length - tail));
            System.arraycopy(data, offset, samples, tail, count);
            offset += count;
            size += count;
        }
    }

    /** Reads up to one playback block and pads underflow with silence. */
    public synchronized int read(short[] target) {
        int count = Math.min(size, target.length);
        int first = Math.min(count, samples.length - head);
        System.arraycopy(samples, head, target, 0, first);
        System.arraycopy(samples, 0, target, first, count - first);
        Arrays.fill(target, count, target.length, (short) 0);
        head = (head + count) % samples.length;
        size -= count;
        notifyAll();
        return count;
    }

    public synchronized int size() { return size; }
    public synchronized void clear() { head = size = 0; notifyAll(); }
    public synchronized void close() { closed = true; clear(); }
}
