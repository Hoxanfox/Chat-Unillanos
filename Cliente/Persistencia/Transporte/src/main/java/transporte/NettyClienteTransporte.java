package transporte;

import dto.gestionConexion.conexion.DTOSesion;
import dto.gestionConexion.transporte.DTOConexion;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Implementación de ITransporte usando Netty con el protocolo LengthField.
 * Este protocolo es compatible con el servidor que usa LengthFieldPrepender/Decoder.
 */
public class NettyClienteTransporte implements ITransporte {

    private static final int MAX_FRAME_SIZE = 100 * 1024 * 1024; // 100MB
    private static final String TAG = "[NettyCliente] ";

    @Override
    public DTOSesion conectar(DTOConexion datosConexion) {
        try {
            // Cola para recibir mensajes del servidor
            BlockingQueue<String> mensajesEntrantes = new LinkedBlockingQueue<>();
            
            // EventLoopGroup para el cliente
            EventLoopGroup group = new NioEventLoopGroup();
            
            // Objeto para mantener la referencia del canal
            final Channel[] channelRef = new Channel[1];

            Bootstrap b = new Bootstrap();
            b.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        
                        // MISMO PROTOCOLO QUE EL SERVIDOR
                        // 1. Decodificador de entrada (lee 4 bytes de tamaño + mensaje)
                        p.addLast(new LengthFieldBasedFrameDecoder(MAX_FRAME_SIZE, 0, 4, 0, 4));
                        
                        // 2. Prepender de salida (agrega 4 bytes con el tamaño antes de enviar)
                        p.addLast(new LengthFieldPrepender(4));
                        
                        // 3. Conversión String <-> Bytes
                        p.addLast(new StringDecoder());
                        p.addLast(new StringEncoder());
                        
                        // 4. Handler personalizado para recibir mensajes
                        p.addLast(new ClienteInboundHandler(mensajesEntrantes));
                    }
                });

            // Conectar de forma síncrona
            ChannelFuture future = b.connect(datosConexion.getHost(), datosConexion.getPuerto()).sync();
            
            if (future.isSuccess()) {
                channelRef[0] = future.channel();
                System.out.println(TAG + "✓ Conexión establecida con " + 
                    datosConexion.getHost() + ":" + datosConexion.getPuerto());
                
                // Crear adaptador compatible con DTOSesion
                NettySessionAdapter adapter = new NettySessionAdapter(
                    channelRef[0], 
                    group, 
                    mensajesEntrantes
                );
                
                return new DTOSesion(adapter.getSocket(), adapter.getOut(), adapter.getIn());
            } else {
                group.shutdownGracefully();
                System.err.println(TAG + "✗ No se pudo conectar al servidor");
                return null;
            }
            
        } catch (Exception e) {
            System.err.println(TAG + "Error al crear la conexión: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}

