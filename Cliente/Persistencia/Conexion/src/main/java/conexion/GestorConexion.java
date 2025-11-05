package conexion;

import dto.gestionConexion.conexion.DTOSesion;

import java.io.IOException;

/**
 * Gestor Singleton que almacena y gestiona el ciclo de vida de la DTOSesion activa.
 */
public class GestorConexion {

    private static GestorConexion instancia;
    private DTOSesion sesionActiva;

    private GestorConexion() {}

    public static synchronized GestorConexion getInstancia() {
        if (instancia == null) {
            instancia = new GestorConexion();
        }
        return instancia;
    }

    public void setSesion(DTOSesion sesion) {
        if (this.sesionActiva != null && this.sesionActiva.estaActiva()) {
            cerrarSesion();
        }
        this.sesionActiva = sesion;
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
    public void cerrarSesion() {
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
            }
        }
    }
}

