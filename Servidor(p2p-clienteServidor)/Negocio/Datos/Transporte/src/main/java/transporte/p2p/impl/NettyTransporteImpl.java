package transporte.p2p.impl;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
// IMPORTANTE: Nuevos decodificadores para evitar JSONs rotos
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
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

    // Aumentamos límite a 50MB para soportar listas grandes de sincronización
    private static final int MAX_FRAME_SIZE = 50 * 1024 * 1024;

    private IMensajeListener listener;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private EventLoopGroup clientGroup; // Grupo compartido para conexiones cliente
    private final Map<String, Channel> canalesActivos = new ConcurrentHashMap<>();

    public NettyTransporteImpl(IMensajeListener listener) {
        this.listener = listener;
    }

    public void setListener(IMensajeListener listener) {
        this.listener = listener;
    }

    /**
     * Configuración centralizada del Pipeline para asegurar que Cliente y Servidor
     * hablen el mismo idioma y no rompan los JSONs.
     * 
     * PROTOCOLO UNIFICADO: LengthField (4 bytes de prefijo con tamaño del mensaje)
     * - Tanto clientes como servidores P2P usan este mismo formato
     */
    private void configurarPipeline(ChannelPipeline p) {
        // 1. ENTRADA: Decodificador inteligente
        // Lee los primeros 4 bytes para saber el tamaño y espera hasta tener todo el mensaje.
        p.addLast(new LengthFieldBasedFrameDecoder(MAX_FRAME_SIZE, 0, 4, 0, 4));

        // 2. SALIDA: Prepender automático
        // Calcula el tamaño del String y le pega 4 bytes al inicio antes de enviarlo.
        p.addLast(new LengthFieldPrepender(4));

        // 3. Conversión Texto <-> Bytes
        p.addLast(new StringDecoder());
        p.addLast(new StringEncoder());

        // 4. Lógica de negocio
        p.addLast(new P2PInboundHandler(listener, canalesActivos));
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
                        configurarPipeline(ch.pipeline());
                    }
                });

        b.bind(puerto).sync();
        System.out.println(TAG + "Servidor escuchando en puerto: " + puerto);
    }

    @Override
    public void conectarA(String host, int puerto) {
        if (host == null || host.trim().isEmpty() || host.equalsIgnoreCase("null")) {
            System.err.println(TAG + ROJO + "Error: Intento de conectar a HOST inválido (" + host + ")." + RESET);
            return;
        }

        String key = host + ":" + puerto;
        if (canalesActivos.containsKey(key) && canalesActivos.get(key).isActive()) return;

        // Usar grupo compartido para evitar crear un EventLoopGroup por cada conexión
        if (clientGroup == null || clientGroup.isShutdown()) {
            clientGroup = new NioEventLoopGroup();
        }

        Bootstrap b = new Bootstrap();
        b.group(clientGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        configurarPipeline(ch.pipeline());
                    }
                });

        b.connect(host, puerto).addListener((ChannelFuture f) -> {
            if (f.isSuccess()) {
                System.out.println(TAG + "Conectado a " + key);
                canalesActivos.put(key, f.channel());
            } else {
                System.err.println(TAG + ROJO + "Error conectando a " + key + ": " + f.cause().getMessage() + RESET);
            }
        });
    }

    @Override
    public void enviarMensaje(String host, int puerto, String mensaje) {
        if (host == null || host.equals("null")) {
            System.err.println(TAG + "No se puede enviar mensaje: Host es NULL.");
            return;
        }

        String key = host + ":" + puerto;
        Channel canal = canalesActivos.get(key);

        if (canal != null && canal.isActive()) {
            // Al usar LengthFieldPrepender, ya no necesitas agregar \r\n manuales.
            // Él se encarga de empaquetar el mensaje perfectamente.
            canal.writeAndFlush(mensaje);
        } else {
            // Reintento de conexión si se cayó
            conectarA(host, puerto);
        }
    }

    @Override
    public void desconectar(String host, int puerto) {
        String key = host + ":" + puerto;
        Channel canal = canalesActivos.remove(key);
        if (canal != null) {
            System.out.println(TAG + "Cerrando conexión con " + key);
            canal.close();
        }
    }

    @Override
    public void detener() {
        System.out.println(TAG + "Deteniendo transporte Netty...");
        
        // Cerrar todos los canales activos primero
        canalesActivos.values().forEach(Channel::close);
        canalesActivos.clear();
        
        // Cerrar grupos de eventos
        if (workerGroup != null && !workerGroup.isShutdown()) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null && !bossGroup.isShutdown()) {
            bossGroup.shutdownGracefully();
        }
        if (clientGroup != null && !clientGroup.isShutdown()) {
            clientGroup.shutdownGracefully();
        }
        
        System.out.println(TAG + "Transporte Netty detenido");
    }
}