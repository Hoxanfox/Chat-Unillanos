package transporte;

import io.netty.channel.Channel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;

/**
 * Wrapper de Socket que encapsula un Channel de Netty.
 * Permite que DTOSesion.estaActiva() funcione correctamente con Netty.
 */
public class NettySocketWrapper extends Socket {
    
    private final Channel channel;
    
    public NettySocketWrapper(Channel channel) {
        this.channel = channel;
    }
    
    @Override
    public boolean isConnected() {
        return channel != null && channel.isActive();
    }
    
    @Override
    public boolean isClosed() {
        return channel == null || !channel.isOpen();
    }
    
    @Override
    public void close() throws IOException {
        if (channel != null) {
            channel.close();
        }
    }
    
    @Override
    public InetAddress getInetAddress() {
        if (channel != null && channel.remoteAddress() instanceof InetSocketAddress) {
            return ((InetSocketAddress) channel.remoteAddress()).getAddress();
        }
        return null;
    }
    
    @Override
    public int getPort() {
        if (channel != null && channel.remoteAddress() instanceof InetSocketAddress) {
            return ((InetSocketAddress) channel.remoteAddress()).getPort();
        }
        return -1;
    }
    
    @Override
    public SocketAddress getRemoteSocketAddress() {
        return channel != null ? channel.remoteAddress() : null;
    }
    
    @Override
    public InputStream getInputStream() throws IOException {
        throw new UnsupportedOperationException("Use BufferedReader from NettySessionAdapter");
    }
    
    @Override
    public OutputStream getOutputStream() throws IOException {
        throw new UnsupportedOperationException("Use PrintWriter from NettySessionAdapter");
    }
}

