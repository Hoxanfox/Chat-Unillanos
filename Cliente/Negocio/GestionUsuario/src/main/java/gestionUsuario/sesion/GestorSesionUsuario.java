package gestionUsuario.sesion;
/**
 * Singleton para gestionar la sesión del usuario activo en toda la aplicación.
 * Almacena información crucial como el ID del usuario que ha iniciado sesión.
 */
public class GestorSesionUsuario {

    private static GestorSesionUsuario instancia;
    private String userId;

    private GestorSesionUsuario() {}

    public static synchronized GestorSesionUsuario getInstancia() {
        if (instancia == null) {
            instancia = new GestorSesionUsuario();
        }
        return instancia;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        if (userId == null) {
            throw new IllegalStateException("No hay un usuario autenticado en la sesión.");
        }
        return userId;
    }

    public boolean haySesionActiva() {
        return userId != null;
    }

    public void cerrarSesion() {
        this.userId = null;
    }
}

