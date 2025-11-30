package conexion;

import dto.gestionConexion.conexion.DTOSesion;
import dto.gestionConexion.DTOEstadoConexion;
import observador.IObservador;
import observador.ISujeto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestor Singleton que almacena y gestiona el ciclo de vida de la DTOSesion activa.
 * Ahora actúa como un "Sujeto" observable para notificar cambios de sesión.
 */
public class GestorConexion implements ISujeto {

    private static GestorConexion instancia;
    private DTOSesion sesionActiva;
    private final List<IObservador> observadores = new ArrayList<>();

    private GestorConexion() {}

    public static synchronized GestorConexion getInstancia() {
        if (instancia == null) {
            instancia = new GestorConexion();
        }
        return instancia;
    }

    public synchronized void setSesion(DTOSesion sesion) {
        if (this.sesionActiva != null && this.sesionActiva.estaActiva()) {
            cerrarSesion();
        }
        this.sesionActiva = sesion;

        // Notificar a observadores el nuevo estado de la conexión
        DTOEstadoConexion estado = buildEstadoDesdeSesion(sesion);
        notificarObservadores("ACTUALIZAR_ESTADO_CONEXION", estado);
    }

    /**
     * Devuelve la sesión de conexión activa.
     * @return la DTOSesion activa, o null si no hay ninguna.
     */
    public DTOSesion getSesion() {
        return sesionActiva;
    }

    /**
     * Cierra la sesión activa y todos sus recursos asociados de forma segura.
     */
    public synchronized void cerrarSesion() {
        if (sesionActiva != null && sesionActiva.estaActiva()) {
            try {
                System.out.println("Cerrando recursos de la sesión activa...");
                if (sesionActiva.getIn() != null) sesionActiva.getIn().close();
                if (sesionActiva.getOut() != null) sesionActiva.getOut().close();
                if (sesionActiva.getSocket() != null) sesionActiva.getSocket().close();
                System.out.println("Recursos de la sesión cerrados correctamente.");
            } catch (IOException e) {
                System.err.println("Error al cerrar los recursos de la sesión: " + e.getMessage());
            } finally {
                this.sesionActiva = null;

                // Notificar a observadores que la sesión se ha cerrado
                DTOEstadoConexion estado = new DTOEstadoConexion(false, "Desconocido", 0, "Desconectado");
                notificarObservadores("ACTUALIZAR_ESTADO_CONEXION", estado);
            }
        }
    }

    private DTOEstadoConexion buildEstadoDesdeSesion(DTOSesion sesion) {
        if (sesion == null || !sesion.estaActiva()) {
            return new DTOEstadoConexion(false, "Desconocido", 0, "Desconectado");
        }
        String servidor = "Desconocido";
        try {
            if (sesion.getSocket() != null && sesion.getSocket().getInetAddress() != null) {
                servidor = sesion.getSocket().getInetAddress().getHostAddress();
            }
        } catch (Exception ignored) {}
        // Ping no disponible desde aquí; se mantiene en 0.
        return new DTOEstadoConexion(true, servidor, 0, "Conectado");
    }

    // --- Implementación de ISujeto ---
    @Override
    public void registrarObservador(IObservador observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
        }
    }

    @Override
    public void removerObservador(IObservador observador) {
        observadores.remove(observador);
    }

    @Override
    public void notificarObservadores(String tipoDeDato, Object datos) {
        for (IObservador observador : new ArrayList<>(observadores)) {
            try {
                observador.actualizar(tipoDeDato, datos);
            } catch (Exception e) {
                System.err.println("Error notificando observador: " + e.getMessage());
            }
        }
    }
}
