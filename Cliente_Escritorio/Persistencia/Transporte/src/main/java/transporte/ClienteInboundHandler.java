package transporte;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.concurrent.BlockingQueue;

/**
 * Handler que recibe mensajes del servidor y los pone en una cola
 * para que puedan ser consumidos por el BufferedReader adaptado.
 */
public class ClienteInboundHandler extends SimpleChannelInboundHandler<String> {

    private static final String TAG = "[ClienteHandler] ";
    private final BlockingQueue<String> mensajesEntrantes;

    public ClienteInboundHandler(BlockingQueue<String> mensajesEntrantes) {
        this.mensajesEntrantes = mensajesEntrantes;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String mensaje) throws Exception {
        // Agregar el mensaje a la cola para que pueda ser leído
        mensajesEntrantes.offer(mensaje);
        System.out.println(TAG + "✓ Mensaje recibido (" + mensaje.length() + " bytes)");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println(TAG + "✓ Canal activo con servidor");
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("[Netty-Inbound] <<< Canal CERRADO con: " + ctx.channel().remoteAddress());
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.err.println("[Netty-Inbound] Excepción en el canal: " + cause.getMessage());
        cause.printStackTrace();
        ctx.close();
    }
}

