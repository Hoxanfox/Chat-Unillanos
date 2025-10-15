package com.unillanos.server.netty.server;

import com.unillanos.server.config.ServerConfigProperties;
import com.unillanos.server.netty.handler.ClientRequestHandler;
import com.unillanos.server.service.impl.ConnectionManager;
import com.unillanos.server.service.interfaces.IActionDispatcher;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Servidor Netty para comunicación TCP/IP asíncrona.
 */
@Component
public class NettyServer {
    
    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);
    
    private final ServerConfigProperties config;
    private final IActionDispatcher actionDispatcher;
    private final ConnectionManager connectionManager;
    private Channel serverChannel;

    public NettyServer(ServerConfigProperties config, IActionDispatcher actionDispatcher, ConnectionManager connectionManager) {
        this.config = config;
        this.actionDispatcher = actionDispatcher;
        this.connectionManager = connectionManager;
    }

    public void start() throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup(config.getNetty().getBossThreads());
        EventLoopGroup workerGroup = new NioEventLoopGroup(config.getNetty().getWorkerThreads());

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            
                            // Decodificador de líneas (mensajes terminan en \n)
                            // Límite aumentado para permitir chunks de archivos grandes (2MB + overhead)
                            pipeline.addLast(new LineBasedFrameDecoder(2 * 1024 * 1024 + 1024)); // 2MB + 1KB overhead
                            
                            // Codificadores/Decodificadores de String
                            pipeline.addLast(new StringDecoder(CharsetUtil.UTF_8));
                            pipeline.addLast(new StringEncoder(CharsetUtil.UTF_8));
                            
                            // Handler de peticiones del cliente
                            pipeline.addLast(new ClientRequestHandler(actionDispatcher, connectionManager));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture future = bootstrap.bind(config.getNetty().getPort()).sync();
            serverChannel = future.channel();
            
            logger.info("Servidor Netty iniciado en el puerto {}", config.getNetty().getPort());
            
            // No bloqueamos aquí, solo configuramos el listener de cierre
            serverChannel.closeFuture().addListener((ChannelFutureListener) closeFuture -> {
                logger.info("Servidor Netty detenido");
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            });
            
        } catch (Exception e) {
            logger.error("Error al iniciar el servidor Netty", e);
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            throw e;
        }
    }

    public void stop() {
        if (serverChannel != null) {
            serverChannel.close();
        }
    }
}

