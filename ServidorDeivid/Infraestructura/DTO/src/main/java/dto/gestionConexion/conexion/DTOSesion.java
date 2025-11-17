package dto.gestionConexion.conexion;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * DTO sencillo que encapsula recursos de una sesión de red.
 * Implementación mínima para compilar el proyecto y satisfacer usos en el código.
 */
public class DTOSesion {
    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;

    public DTOSesion(Socket socket, PrintWriter out, BufferedReader in) {
        this.socket = socket;
        this.out = out;
        this.in = in;
    }

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
     * Estado simple: activa si socket no es null y no está cerrado.
     */
    public boolean estaActiva() {
        try {
            return socket != null && !socket.isClosed();
        } catch (Exception e) {
            return false;
        }
    }
}

