package net.somewhatcity.boiler.display.sources.boilerClientSource;

import de.maxhenkel.voicechat.api.VoicechatServerApi;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.somewhatcity.boiler.BoilerVoicechatPlugin;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.InflaterInputStream;

public class BoilerServerHandler extends ChannelInboundHandlerAdapter {

    private static BufferedImage image;
    private BoilerClientSource boilerSource;
    private VoicechatServerApi serverApi = (VoicechatServerApi) BoilerVoicechatPlugin.voicechatApi;

    private final AudioFormat SOURCE_FORMAT = new AudioFormat(48000, 16, 1, true, true);
    private final AudioFormat TARGET_FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 48000F, 16, 1, 2, 48000F, false);

    public BoilerServerHandler(BoilerClientSource source) {
        this.boilerSource = source;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf byteBuf = (ByteBuf) msg;
            byte[] data = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(data);
            byteBuf.release();

            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            byte[] bytes = inputStream.readAllBytes();
            if(bytes[0] == 0b00000000) {
                byte[] imageData = new byte[bytes.length - 1];
                System.arraycopy(bytes, 1, imageData, 0, bytes.length - 1);
                image = ImageIO.read(new ByteArrayInputStream(imageData));
            } else if (bytes[0] == 0b00000001) {
                byte[] audioData = new byte[bytes.length - 1];
                System.arraycopy(bytes, 1, audioData, 0, bytes.length - 1);
                /*
                AudioInputStream source = new AudioInputStream(new ByteArrayInputStream(audioData), SOURCE_FORMAT, audioData.length / SOURCE_FORMAT.getFrameSize());
                AudioInputStream converted = AudioSystem.getAudioInputStream(TARGET_FORMAT, source);

                 */
                short[] audio = serverApi.getAudioConverter().bytesToShorts(audioData);

                boilerSource.emptyQueue();
                for(int i = 0; i < 25; i++) {
                    short[] shortend = new short[960];
                    System.arraycopy(audio, i * 960, shortend, 0, 960);
                    boilerSource.queueAudio(shortend);
                }
            } else {
                System.out.println("Received unknown data from server");
            }
            //image = ImageIO.read(inputStream);
            inputStream.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    public static BufferedImage getImage() {
        return image;
    }
}
