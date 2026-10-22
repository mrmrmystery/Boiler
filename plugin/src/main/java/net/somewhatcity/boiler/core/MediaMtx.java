/*
 * Copyright (c) 2025.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.core;

import net.somewhatcity.boiler.core.runtime.AsyncCache;
import okhttp3.Request;
import okhttp3.Response;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class MediaMtx {
    private Process process;
    private Thread worker;
    private boolean stopped;
    private final AsyncCache<String, StreamState> states = new AsyncCache<>(BoilerPlugin.getPlugin().runtime().io(), 250);

    public synchronized void start() {
        if (worker != null || stopped) return;
        worker = BoilerPlugin.getPlugin().runtime().startWorker("mediamtx", () -> {
            Process child = null;
            try {
                File directory = BoilerPlugin.getPlugin().getDataFolder();
                String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
                String binary = os.contains("win") ? "mediamtx.exe" : "mediamtx";
                synchronized (this) {
                    if (stopped) return;
                    child = new ProcessBuilder(new File(directory, binary).getAbsolutePath()).directory(directory).inheritIO().start();
                    process = child;
                }
                child.waitFor();
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            catch (IOException e) { BoilerPlugin.getPlugin().getLogger().warning("MediaMTX: " + e.getMessage()); }
            finally { if (child != null && child.isAlive()) destroy(child); }
        });
    }

    public synchronized void stop() {
        if (stopped) return;
        stopped = true;
        states.close();
        if (process != null) destroy(process);
        if (worker != null) worker.interrupt();
    }

    private static void destroy(Process process) {
        process.descendants().forEach(ProcessHandle::destroy);
        process.destroy();
        if (process.isAlive()) process.destroyForcibly();
    }

    public CompletableFuture<StreamState> getStreamStateAsync(String path) {
        return states.get(path, () -> getStreamState(path)).exceptionally(error -> StreamState.UNKNOWN);
    }

    public StreamState getStreamState(String path) {
        Request request = new Request.Builder().url("http://127.0.0.1:9997/v3/paths/get/" + path).build();
        try (Response response = BoilerPlugin.getPlugin().runtime().http().newCall(request).execute()) {
            if (response.code() == 200) return StreamState.ONLINE;
            if (response.code() == 404) return StreamState.OFFLINE;
        } catch (IOException e) { return StreamState.UNKNOWN; }
        return StreamState.UNKNOWN;
    }
    public enum StreamState { ONLINE, OFFLINE, UNKNOWN }
}
