package net.somewhatcity.boiler.core.sources;

/** Cancellation that is safe while load() is still constructing the source. */
public interface CancellableLoad {
    void cancelLoad();
}
