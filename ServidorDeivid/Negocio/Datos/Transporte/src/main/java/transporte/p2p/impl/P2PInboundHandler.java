package transporte.p2p.impl;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import transporte.p2p.interfaces.IMensajeListener;

import java.net.InetSocketAddress;
import java.util.Map;

public class P2PInboundHandler extends SimpleChannelInboundHandler<String> {

    // --- COLORES ANSI ---
    private static final String RESET = "\u001B[0m";
    private static final String VERDE = "\u001B[32m";
    private static final String AMARILLO = "\u001B[33m";
    private static final String ROJO = "\u001B[31m";
    private static final String CYAN = "\u001B[36m";

    private static final String TAG = CYAN + "[Netty-Inbound] " + RESET;

    private final IMensajeListener listener;
    private final Map<String, Channel> canales;

    public P2PInboundHandler(IMensajeListener listener, Map<String, Channel> canales) {
        this.listener = listener;
        this.canales = canales;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // Registramos la conexión entrante o saliente
        InetSocketAddress socketAddr = (InetSocketAddress) ctx.channel().remoteAddress();
        String host = socketAddr.getHostString();
        int port = socketAddr.getPort();
        String key = host + ":" + port;

        System.out.println(TAG + VERDE + ">>> Canal ACTIVO con: " + key + RESET);

        canales.put(key, ctx.channel());

        if (listener != null) {
            listener.onNuevaConexion(key);
        }
        super.channelActive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        InetSocketAddress socketAddr = (InetSocketAddress) ctx.channel().remoteAddress();
        String origen = socketAddr.getHostString() + ":" + socketAddr.getPort();

        // Log de tráfico (truncado si es muy largo para no saturar)
        String msgLog = msg.length() > 100 ? msg.substring(0, 100) + "..." : msg;
        System.out.println(TAG + AMARILLO + "DATA recibida de " + origen + ": " + RESET + msgLog);

        if (listener != null) {
            listener.onMensajeRecibido(msg, origen);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        InetSocketAddress socketAddr = (InetSocketAddress) ctx.channel().remoteAddress();
        String key = socketAddr.getHostString() + ":" + socketAddr.getPort();

        System.out.println(TAG + ROJO + "<<< Canal CERRADO con: " + key + RESET);
        canales.remove(key);
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.err.println(TAG + ROJO + "Excepción en el canal: " + cause.getMessage() + RESET);
        // cause.printStackTrace(); // Descomentar para full stacktrace
        ctx.close();
    }
}