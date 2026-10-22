package net.somewhatcity.boiler.core;

import com.sun.management.ThreadMXBean;
import net.somewhatcity.boiler.api.IBoilerSource;
import net.somewhatcity.boiler.core.display.RasterRenderer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import java.util.List;

/** Narrow allocation/CPU benchmark, deliberately independent of server and packet costs. */
public final class RasterBenchmark {
    private static volatile int checksum;
    private record Result(String mode, int displays, int width, int height, boolean viewers,
                          long nanos, long cpuNanos, long bytes, long gcCount, long gcMillis, int threads) {
        String csv() {
            return String.format(Locale.ROOT, "%s,%d,%d,%d,%s,%.3f,%.3f,%d,%d,%d,%d",
                    mode, displays, width, height, viewers, nanos / 1e6, cpuNanos / 1e6, bytes, gcCount, gcMillis, threads);
        }
    }

    public static void main(String[] args) throws Exception {
        List<String> csv = new ArrayList<>();
        csv.add("mode,displays,width,height,viewers,wall_ms,cpu_ms,allocated_bytes,gc_count,gc_ms,threads");
        // Alternate measurement order between scenarios to reduce consistent JIT/order bias.
        int scenario = 0;
        for (int[] size : new int[][]{{384, 128}, {1024, 576}}) {
            for (int count : new int[]{1, 10, 50}) for (boolean viewers : new boolean[]{false, true}) {
                boolean[] order = scenario++ % 2 == 0 ? new boolean[]{false, true} : new boolean[]{true, false};
                for (boolean reuse : order) {
                    Result result = measure(reuse, count, size[0], size[1], viewers);
                    csv.add(result.csv());
                    System.out.println(result.csv());
                }
            }
        }
        Path output = Path.of(args[0]);
        Files.createDirectories(output.getParent());
        Files.write(output, csv);
    }

    private static Result measure(boolean reuse, int count, int width, int height, boolean viewers) throws Exception {
        RasterRenderer[] renderers = new RasterRenderer[count];
        IBoilerSource[] sources = new IBoilerSource[count];
        for (int i = 0; i < count; i++) {
            renderers[i] = new RasterRenderer();
            sources[i] = i % 4 == 3 ? RenderingTest.keyboard() : content(i % 4);
        }
        for (int i = 0; i < 20; i++) frame(reuse, viewers, renderers, sources, width, height);
        ThreadMXBean thread = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        thread.setThreadAllocatedMemoryEnabled(true);
        long id = Thread.currentThread().threadId();
        long allocated = thread.getThreadAllocatedBytes(id), cpu = thread.getCurrentThreadCpuTime();
        long gcCount = gcCount(), gcMillis = gcMillis(), start = System.nanoTime();
        for (int i = 0; i < 60; i++) frame(reuse, viewers, renderers, sources, width, height);
        return new Result(reuse ? "reused" : "original-adapter", count, width, height, viewers,
                System.nanoTime() - start, thread.getCurrentThreadCpuTime() - cpu,
                thread.getThreadAllocatedBytes(id) - allocated, gcCount() - gcCount, gcMillis() - gcMillis,
                ManagementFactory.getThreadMXBean().getThreadCount());
    }

    private static void frame(boolean reuse, boolean viewers, RasterRenderer[] renderers, IBoilerSource[] sources, int width, int height) {
        if (!viewers) return;
        for (int i = 0; i < sources.length; i++) {
            BufferedImage image;
            if (reuse) image = renderers[i].drawImage(sources[i], width, height);
            else {
                // The original IBoilerSource adapter allocated this image for every draw.
                image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = image.createGraphics();
                try { sources[i].draw(graphics, new Rectangle(0, 0, width, height)); }
                finally { graphics.dispose(); }
            }
            checksum ^= image.getRGB(width / 2, height / 2);
        }
    }

    private static IBoilerSource content(int kind) {
        BufferedImage texture = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D initial = texture.createGraphics();
        try {
            initial.setColor(new Color(0x3562a3)); initial.fillRect(0, 0, 64, 64);
            initial.setColor(new Color(0xffba53)); initial.fillOval(4, 8, 40, 40);
        } finally { initial.dispose(); }
        return new RenderingTest.Source() {
            int frame;
            @Override public void draw(Graphics2D graphics, Rectangle viewport) {
                graphics.drawImage(texture, 0, 0, viewport.width, viewport.height, null);
                if (kind > 0) {
                    graphics.setColor(new Color(0xff000000 | (++frame * 123457 & 0xffffff), true));
                    graphics.fillRect(frame % Math.max(1, viewport.width - 8), 2, 8, 8);
                }
            }
        };
    }
    private static long gcCount() { return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(bean -> Math.max(0, bean.getCollectionCount())).sum(); }
    private static long gcMillis() { return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(bean -> Math.max(0, bean.getCollectionTime())).sum(); }
}
