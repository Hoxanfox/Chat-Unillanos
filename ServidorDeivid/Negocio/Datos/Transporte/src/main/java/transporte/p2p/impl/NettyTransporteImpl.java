package transporte.p2p.impl;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import transporte.p2p.interfaces.IMensajeListener;
import transporte.p2p.interfaces.ITransporteTcp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NettyTransporteImpl implements ITransporteTcp {

    // 1. IMPORTANTE: Ya NO es 'final', porque se setea después del constructor
    private IMensajeListener listener;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private final Map<String, Channel> canalesActivos = new ConcurrentHashMap<>();

    public NettyTransporteImpl(IMensajeListener listener) {
        this.listener = listener;
    }

    // 2. IMPORTANTE: El método que te faltaba
    public void setListener(IMensajeListener listener) {
        this.listener = listener;
    }

    @Override
    public void iniciarEscucha(int puerto) throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new StringDecoder());
                        ch.pipeline().addLast(new StringEncoder());
                        // Aquí pasamos 'listener' (que ya no será null cuando esto corra)
                        ch.pipeline().addLast(new P2PInboundHandler(listener, canalesActivos));
                    }
                });

        b.bind(puerto).sync();
        System.out.println(">> [Netty] Escuchando en puerto: " + puerto);
    }

    @Override
    public void conectarA(String host, int puerto) {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new StringDecoder());
                            ch.pipeline().addLast(new StringEncoder());
                            ch.pipeline().addLast(new P2PInboundHandler(listener, canalesActivos));
                        }
                    });

            ChannelFuture f = b.connect(host, puerto).sync();
            String key = host + ":" + puerto;
            canalesActivos.put(key, f.channel());
            System.out.println(">> [Netty] Conectado a " + key);

        } catch (Exception e) {
            System.err.println(">> [Netty] Error conectando a " + host + ": " + e.getMessage());
        }
    }

    @Override
    public void enviarMensaje(String host, int puerto, String mensaje) {
        String key = host + ":" + puerto;
        Channel canal = canalesActivos.get(key);
        if (canal != null && canal.isActive()) {
            canal.writeAndFlush(mensaje);
        } else {
            // Intento de reconexión rápida si quieres
            System.out.println(">> [Netty] Reconectando para enviar a " + key);
            conectarA(host, puerto);
            // Nota: El mensaje actual podría perderse si no implementas una cola,
            // pero para este ejemplo es suficiente.
        }
    }

    @Override
    public void detener() {
        if (workerGroup != null) workerGroup.shutdownGracefully();
        if (bossGroup != null) bossGroup.shutdownGracefully();
    }
}