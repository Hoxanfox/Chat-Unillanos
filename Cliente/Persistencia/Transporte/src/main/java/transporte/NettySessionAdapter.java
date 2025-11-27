package transporte;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Adaptador que convierte un Channel de Netty en PrintWriter/BufferedReader
 * para mantener compatibilidad con el código existente del cliente.
 */
public class NettySessionAdapter {

    private final Channel channel;
    private final EventLoopGroup group;
    private final PrintWriter out;
    private final BufferedReader in;
    private final Socket socketWrapper;

    public NettySessionAdapter(Channel channel, EventLoopGroup group, BlockingQueue<String> mensajesEntrantes) {
        this.channel = channel;
        this.group = group;
        this.socketWrapper = new NettySocketWrapper(channel);

        // Adaptar el canal Netty a PrintWriter
        this.out = new PrintWriter(new Writer() {
            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                String mensaje = new String(cbuf, off, len);
                if (channel.isActive()) {
                    channel.writeAndFlush(mensaje);
                }
            }

            @Override
            public void flush() throws IOException {
                channel.flush();
            }

            @Override
            public void close() throws IOException {
                channel.close();
                group.shutdownGracefully();
            }
        }, true); // autoFlush = true

        // Adaptar la cola de mensajes a BufferedReader
        this.in = new BufferedReader(new java.io.Reader() {
            @Override
            public int read(char[] cbuf, int off, int len) throws IOException {
                try {
                    String mensaje = mensajesEntrantes.poll(1, TimeUnit.SECONDS);
                    if (mensaje == null) {
                        return 0; // Timeout
                    }
                    
                    char[] chars = mensaje.toCharArray();
                    int toCopy = Math.min(chars.length, len);
                    System.arraycopy(chars, 0, cbuf, off, toCopy);
                    return toCopy;
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted", e);
                }
            }

            @Override
            public void close() throws IOException {
                channel.close();
                group.shutdownGracefully();
            }
        }) {
            @Override
            public String readLine() throws IOException {
                try {
                    // Bloquear hasta recibir un mensaje
                    String mensaje = mensajesEntrantes.take();
                    return mensaje;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted", e);
                }
            }
        };
    }

    public PrintWriter getOut() {
        return out;
    }

    public BufferedReader getIn() {
        return in;
    }

    public Socket getSocket() {
        return socketWrapper;
    }
}

