package servidor;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;

/**
 * Servidor TCP usando Netty.
 * Configura el pipeline de handlers para procesar mensajes basados en líneas (JSON).
 */
public class ServidorNetty {
    
    private final int puerto;
    
    public ServidorNetty(int puerto) {
        this.puerto = puerto;
    }
    
    /**
     * Inicia el servidor y escucha conexiones entrantes.
     */
    public void iniciar() throws Exception {
        // Grupo de hilos para aceptar conexiones
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        
        // Grupo de hilos para manejar el I/O de las conexiones
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel canal) throws Exception {
                        // Pipeline de handlers para procesar mensajes
                        canal.pipeline()
                            // 1. LineBasedFrameDecoder: Divide el stream en mensajes usando '\n' como delimitador
                            .addLast(new LineBasedFrameDecoder(8192))
                            
                            // 2. StringDecoder: Convierte bytes a String
                            .addLast(new StringDecoder(CharsetUtil.UTF_8))
                            
                            // 3. StringEncoder: Convierte String a bytes
                            .addLast(new StringEncoder(CharsetUtil.UTF_8))
                            
                            // 4. ManejadorCliente: Nuestra lógica de negocio
                            .addLast(new ManejadorCliente());
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
            
            // Enlazar y comenzar a aceptar conexiones entrantes
            System.out.println("╔════════════════════════════════════════════════╗");
            System.out.println("║   SERVIDOR CHAT-UNILLANOS INICIADO            ║");
            System.out.println("╚════════════════════════════════════════════════╝");
            System.out.println("🚀 Escuchando en puerto: " + puerto);
            System.out.println("⏳ Esperando conexiones de clientes...\n");
            
            ChannelFuture futuro = bootstrap.bind(puerto).sync();
            
            // Esperar hasta que el canal del servidor se cierre
            futuro.channel().closeFuture().sync();
            
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            System.out.println("🛑 Servidor detenido.");
        }
    }
}

