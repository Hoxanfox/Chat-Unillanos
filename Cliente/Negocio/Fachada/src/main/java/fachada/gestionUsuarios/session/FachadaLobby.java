package fachada.gestionUsuarios.session;

import dto.vistaLobby.DTOUsuario;
import gestionUsuario.sesion.GestorSesionUsuario;
import gestionUsuario.especialista.EspecialistaUsuariosImpl;
import java.util.UUID;

public class FachadaLobby implements IFachadaLobby {

    private final GestorSesionUsuario gestorSesion;
    private final EspecialistaUsuariosImpl especialistaUsuarios;

    public FachadaLobby() {
        this.gestorSesion = GestorSesionUsuario.getInstancia();
        this.especialistaUsuarios = new EspecialistaUsuariosImpl();
        System.out.println("✅ [FachadaLobby]: Inicializada.");
    }

    @Override
    public String obtenerUserIdSesion() {
        System.out.println("[FachadaLobby] Obteniendo userId de la sesión activa...");
        String userId = gestorSesion.getUserId();
        System.out.println("[FachadaLobby] UserId obtenido: " + userId);
        return userId;
    }

    @Override
    public boolean haySesionActiva() {
        boolean activa = gestorSesion.haySesionActiva();
        System.out.println("[FachadaLobby] Verificación de sesión activa: " + activa);
        return activa;
    }

    @Override
    public DTOUsuario cargarInformacionUsuarioLogueado() {
        System.out.println("[FachadaLobby] Cargando información del usuario logueado...");

        if (!haySesionActiva()) {
            System.err.println("[FachadaLobby] ERROR: No hay sesión activa");
            throw new IllegalStateException("No hay un usuario autenticado en la sesión.");
        }

        String userId = obtenerUserIdSesion();
        System.out.println("[FachadaLobby] Consultando datos del usuario: " + userId);

        UUID userIdUUID;
        try {
            userIdUUID = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            System.err.println("[FachadaLobby] ERROR: userId inválido: " + userId);
            throw new IllegalStateException("El userId de la sesión no es un UUID válido.", e);
        }

        DTOUsuario dtoUsuario = especialistaUsuarios.obtenerUsuarioPorIdComoDTO(userIdUUID);

        if (dtoUsuario == null) {
            System.err.println("[FachadaLobby] ERROR: Usuario no encontrado en BD para userId: " + userId);
            throw new IllegalStateException("El usuario de la sesión no existe en la base de datos.");
        }

        System.out.println("[FachadaLobby] Información del usuario cargada exitosamente");
        return dtoUsuario;
    }

    @Override
    public void cerrarSesion() {
        System.out.println("[FachadaLobby] Cerrando sesión del usuario...");
        gestorSesion.cerrarSesion();
        System.out.println("[FachadaLobby] Sesión cerrada exitosamente.");
    }
}
