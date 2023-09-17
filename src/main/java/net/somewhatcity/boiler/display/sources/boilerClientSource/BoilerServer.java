package net.somewhatcity.boiler.display.sources.boilerClientSource;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

public class BoilerServer {

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel channel;

    public BoilerServer(BoilerClientSource source) {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(
                                    new LengthFieldPrepender(4),
                                    new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4),
                                    new BoilerServerHandler(source)
                            );
                        }
                    });

            ChannelFuture future = bootstrap.bind(4090).sync();
            channel = future.channel();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void shutdown() {
        try {
            workerGroup.shutdownGracefully().sync();
            bossGroup.shutdownGracefully().sync();
            channel.closeFuture().sync();
            channel.close();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

}
