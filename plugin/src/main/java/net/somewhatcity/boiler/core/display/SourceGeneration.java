package net.somewhatcity.boiler.core.display;

/** Separates cancellation from publication, including delayed callbacks from old sources. */
public final class SourceGeneration {
    private long value;
    private boolean closed;
    public synchronized long next() { return closed ? -1 : ++value; }
    public synchronized boolean current(long generation) { return !closed && value == generation; }
    public synchronized void close() { closed = true; ++value; }
}
