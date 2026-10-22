package net.somewhatcity.boiler.core.sources;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

public final class BgrxFrames {
    private BgrxFrames() {}
    public static void copy(ByteBuffer input, BufferedImage target, int stride) {
        int width = target.getWidth(), height = target.getHeight();
        if (stride < width * 4 || input.remaining() < (long) (height - 1) * stride + width * 4)
            throw new IllegalArgumentException("Incomplete BGRx frame");
        int[] pixels = ((DataBufferInt) target.getRaster().getDataBuffer()).getData();
        ByteBuffer bytes = input.slice().order(ByteOrder.LITTLE_ENDIAN);
        if (stride == width * 4) bytes.asIntBuffer().get(pixels, 0, width * height);
        else for (int row = 0; row < height; row++) {
            bytes.position(row * stride);
            IntBuffer values = bytes.slice().order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
            values.get(pixels, row * width, width);
        }
    }
}
