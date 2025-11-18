package dto.gestionConexion.conexion;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * DTO sencillo que encapsula recursos de una sesión de red.
 * Implementación mínima para compilar el proyecto y satisfacer usos en el código.
 */
public class DTOSesion {
    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;
    // Indica si existe un lector activo asociado a esta sesión (evita lectores duplicados)
    private final AtomicBoolean lectorActivo = new AtomicBoolean(false);

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

    // --- control de lector asociado ---
    /**
     * Intenta marcar esta sesión como teniendo un lector asociado.
     * @return true si se marcó correctamente (no había lector), false si ya había un lector.
     */
    public boolean intentarAsignarLector() {
        return lectorActivo.compareAndSet(false, true);
    }

    /**
     * Libera la marca de lector activo (debe llamarse cuando el lector finaliza).
     */
    public void liberarLector() {
        lectorActivo.set(false);
    }

    /**
     * Indica si existe actualmente un lector activo en esta sesión.
     */
    public boolean tieneLectorActivo() {
        return lectorActivo.get();
    }
}
