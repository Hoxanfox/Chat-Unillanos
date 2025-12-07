package dto.gestionConexion.conexion;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * DTO que contiene los recursos puros de una sesión de comunicación activa.
 * Este objeto es inmutable y transporta los elementos de bajo nivel de una conexión
 * para ser gestionados por las capas superiores sin crear dependencias cíclicas.
 */
public final class DTOSesion {

    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;

    public DTOSesion(Socket socket, PrintWriter out, BufferedReader in) {
        this.socket = socket;
        this.out = out;
        this.in = in;
    }

    // --- Getters ---

    public Socket getSocket() {
        return socket;
    }

    public PrintWriter getOut() {
        return out;
    }

    public BufferedReader getIn() {
        return in;
    }

    /**
     * Verifica si la sesión tiene una conexión activa.
     * @return true si la conexión está establecida, false en caso contrario.
     */
    public boolean estaActiva() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}
