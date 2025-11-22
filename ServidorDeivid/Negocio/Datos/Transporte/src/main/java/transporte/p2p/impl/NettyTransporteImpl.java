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

    private static final String TAG = "\u001B[34m[Netty-Core] \u001B[0m";
    private static final String ROJO = "\u001B[31m";
    private static final String RESET = "\u001B[0m";

    private IMensajeListener listener;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private final Map<String, Channel> canalesActivos = new ConcurrentHashMap<>();

    public NettyTransporteImpl(IMensajeListener listener) {
        this.listener = listener;
    }

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
                        ch.pipeline().addLast(new P2PInboundHandler(listener, canalesActivos));
                    }
                });

        b.bind(puerto).sync();
        System.out.println(TAG + "Servidor escuchando en puerto: " + puerto);
    }

    @Override
    public void conectarA(String host, int puerto) {
        // PROTECCIÓN CONTRA NULL
        if (host == null || host.trim().isEmpty() || host.equals("null")) {
            System.err.println(TAG + ROJO + "Error: Intento de conectar a HOST inválido (null)." + RESET);
            return;
        }

        String key = host + ":" + puerto;
        if (canalesActivos.containsKey(key) && canalesActivos.get(key).isActive()) return;

        EventLoopGroup group = new NioEventLoopGroup();
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

        b.connect(host, puerto).addListener((ChannelFuture f) -> {
            if (f.isSuccess()) {
                System.out.println(TAG + "Conectado a " + key);
                canalesActivos.put(key, f.channel());
            } else {
                // System.err.println(TAG + "Fallo al conectar a " + key);
                group.shutdownGracefully();
            }
        });
    }

    @Override
    public void enviarMensaje(String host, int puerto, String mensaje) {
        if (host == null) {
            System.err.println(TAG + "No se puede enviar mensaje: Host es NULL.");
            return;
        }

        String key = host + ":" + puerto;
        Channel canal = canalesActivos.get(key);

        if (canal != null && canal.isActive()) {
            canal.writeAndFlush(mensaje);
        } else {
            // System.out.println(TAG + "Reconectando para enviar a " + key);
            conectarA(host, puerto);
        }
    }

    @Override
    public void detener() {
        if (workerGroup != null) workerGroup.shutdownGracefully();
        if (bossGroup != null) bossGroup.shutdownGracefully();
        canalesActivos.clear();
    }
}