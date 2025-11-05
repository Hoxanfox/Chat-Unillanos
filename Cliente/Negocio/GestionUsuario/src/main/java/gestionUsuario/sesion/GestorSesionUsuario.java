package gestionUsuario.sesion;

import dominio.Usuario;

/**
 * Singleton para gestionar la sesión del usuario activo en toda la aplicación.
 * Almacena información crucial como el ID del usuario que ha iniciado sesión.
 */
public class GestorSesionUsuario {

    private static GestorSesionUsuario instancia;
    private String userId;
    private String peerId;  // ← NUEVO: ID del peer WebRTC
    private Usuario usuarioLogueado;

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

    /**
     * Establece el ID del peer WebRTC para la sesión actual.
     * @param peerId El UUID del peer asignado por el servidor
     */
    public void setPeerId(String peerId) {
        this.peerId = peerId;
    }

    /**
     * Obtiene el ID del peer WebRTC de la sesión actual.
     * @return El peer ID o null si no está establecido
     */
    public String getPeerId() {
        return peerId;
    }

    public void setUsuarioLogueado(Usuario usuario) {
        this.usuarioLogueado = usuario;
    }

    public Usuario getUsuarioLogueado() {
        if (usuarioLogueado == null) {
            throw new IllegalStateException("No hay un usuario logueado en la sesión.");
        }
        return usuarioLogueado;
    }

    public boolean haySesionActiva() {
        return userId != null;
    }

    public void cerrarSesion() {
        this.userId = null;
        this.peerId = null;  // ← Limpiar también el peerId
        this.usuarioLogueado = null;
    }
}
