package transporte.p2p.impl;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
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

    // Tamaño máximo de un mensaje JSON (ej. 128KB). Aumentar si la BD crece mucho.
    private static final int MAX_FRAME_SIZE = 131072;

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
                        // 1. DECODIFICADOR DE FRAMES (CRÍTICO PARA JSON)
                        // Corta el flujo cuando encuentra un \n o \r\n
                        ch.pipeline().addLast(new DelimiterBasedFrameDecoder(MAX_FRAME_SIZE, Delimiters.lineDelimiter()));

                        // 2. Decodificador de String normal
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
        if (host == null || host.trim().isEmpty() || host.equalsIgnoreCase("null")) {
            System.err.println(TAG + ROJO + "Error: Intento de conectar a HOST inválido." + RESET);
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
                        // MISMA CONFIGURACIÓN EN EL CLIENTE
                        ch.pipeline().addLast(new DelimiterBasedFrameDecoder(MAX_FRAME_SIZE, Delimiters.lineDelimiter()));
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
                group.shutdownGracefully();
            }
        });
    }

    @Override
    public void enviarMensaje(String host, int puerto, String mensaje) {
        if (host == null) return;

        String key = host + ":" + puerto;
        Channel canal = canalesActivos.get(key);

        if (canal != null && canal.isActive()) {
            // IMPORTANTÍSIMO: Agregar salto de línea al final para que el receptor sepa dónde termina el JSON.
            // Si el mensaje ya tiene \n, Netty lo manejará bien, pero aseguramos el delimitador.
            String mensajeConDelimitador = mensaje + "\r\n";
            canal.writeAndFlush(mensajeConDelimitador);
        } else {
            conectarA(host, puerto);
        }
    }

    @Override
    public void desconectar(String host, int puerto) {
        String key = host + ":" + puerto;
        Channel canal = canalesActivos.remove(key);
        if (canal != null) {
            // System.out.println(TAG + "Cerrando conexión con " + key);
            canal.close();
        }
    }

    @Override
    public void detener() {
        if (workerGroup != null) workerGroup.shutdownGracefully();
        if (bossGroup != null) bossGroup.shutdownGracefully();
        canalesActivos.values().forEach(Channel::close);
        canalesActivos.clear();
    }
}