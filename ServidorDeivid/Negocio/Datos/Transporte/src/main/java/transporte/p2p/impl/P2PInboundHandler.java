package transporte.p2p.impl;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import transporte.p2p.interfaces.IMensajeListener;

import java.net.InetSocketAddress;
import java.util.Map;

public class P2PInboundHandler extends SimpleChannelInboundHandler<String> {

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
        String key = socketAddr.getHostString() + ":" + socketAddr.getPort();

        canales.put(key, ctx.channel());
        listener.onNuevaConexion(key);
        super.channelActive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        // Aquí llega el mensaje TCP. Lo pasamos a la capa superior.
        InetSocketAddress socketAddr = (InetSocketAddress) ctx.channel().remoteAddress();
        String origen = socketAddr.getHostString() + ":" + socketAddr.getPort();

        listener.onMensajeRecibido(msg, origen);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}