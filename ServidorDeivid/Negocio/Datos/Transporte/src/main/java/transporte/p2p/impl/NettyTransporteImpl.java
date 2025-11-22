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

    // --- COLORES ANSI ---
    private static final String RESET = "\u001B[0m";
    private static final String VERDE = "\u001B[32m";
    private static final String AMARILLO = "\u001B[33m";
    private static final String ROJO = "\u001B[31m";
    private static final String AZUL = "\u001B[34m";
    private static final String MAGENTA = "\u001B[35m";

    private static final String TAG = AZUL + "[Netty-Core] " + RESET;

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

        System.out.println(TAG + "Configurando servidor en puerto " + MAGENTA + puerto + RESET + "...");

        try {
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

            ChannelFuture f = b.bind(puerto).sync();
            System.out.println(TAG + VERDE + "✔ Servidor ESCUCHANDO en puerto: " + puerto + RESET);

            // Esperar hasta que el socket del servidor se cierre
            // f.channel().closeFuture().sync(); // Bloqueante, cuidado en Main Thread

        } catch (Exception e) {
            System.err.println(TAG + ROJO + "✘ Error fatal iniciando servidor: " + e.getMessage() + RESET);
            throw e;
        }
    }

    @Override
    public void conectarA(String host, int puerto) {
        if (host == null) {
            System.err.println(TAG + ROJO + "Intento de conexión a HOST NULO abortado." + RESET);
            return;
        }

        String key = host + ":" + puerto;
        if (canalesActivos.containsKey(key) && canalesActivos.get(key).isActive()) {
            // System.out.println(TAG + "Ya existe conexión activa con " + key + ". Omitiendo.");
            return;
        }

        EventLoopGroup group = new NioEventLoopGroup();
        System.out.println(TAG + "Iniciando conexión saliente a: " + AMARILLO + key + RESET);

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

            ChannelFuture f = b.connect(host, puerto);

            f.addListener((ChannelFuture future) -> {
                if (future.isSuccess()) {
                    System.out.println(TAG + VERDE + "✔ Conexión EXITOSA con " + key + RESET);
                    canalesActivos.put(key, future.channel());
                } else {
                    System.err.println(TAG + ROJO + "✘ FALLÓ conexión a " + key + ": " + future.cause().getMessage() + RESET);
                    group.shutdownGracefully(); // Limpiar recursos si falla
                }
            });

        } catch (Exception e) {
            System.err.println(TAG + ROJO + "Excepción al intentar conectar a " + host + ": " + e.getMessage() + RESET);
        }
    }

    @Override
    public void enviarMensaje(String host, int puerto, String mensaje) {
        String key = host + ":" + puerto;
        Channel canal = canalesActivos.get(key);

        if (canal != null && canal.isActive()) {
            // Log ligero para envío
            // System.out.println(TAG + "Enviando a " + key + " (" + mensaje.length() + " bytes)");
            canal.writeAndFlush(mensaje);
        } else {
            System.out.println(TAG + AMARILLO + "⚠ Canal inactivo o inexistente para " + key + ". Intentando reconectar..." + RESET);
            // Intento de reconexión rápida
            conectarA(host, puerto);
            // Nota: El mensaje actual se pierde en este diseño simple, se requeriría una cola de reintentos.
        }
    }

    @Override
    public void detener() {
        System.out.println(TAG + MAGENTA + "Deteniendo hilos de transporte..." + RESET);
        if (workerGroup != null) workerGroup.shutdownGracefully();
        if (bossGroup != null) bossGroup.shutdownGracefully();
        canalesActivos.values().forEach(Channel::close);
        canalesActivos.clear();
        System.out.println(TAG + "Transporte detenido.");
    }
}