package fachada.gestionUsuarios.session;

import com.google.gson.JsonObject;
import comunicacion.EnviadorPeticiones;
import comunicacion.GestorRespuesta;
import comunicacion.IEnviadorPeticiones;
import comunicacion.IGestorRespuesta;
import dto.comunicacion.DTORequest;
import dto.comunicacion.DTOResponse;
import dto.vistaLobby.DTOLogout;
import dto.vistaLobby.DTOUsuario;
import gestionUsuario.sesion.GestorSesionUsuario;
import gestionUsuario.especialista.EspecialistaUsuariosImpl;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class FachadaLobby implements IFachadaLobby {

    private final GestorSesionUsuario gestorSesion;
    private final EspecialistaUsuariosImpl especialistaUsuarios;
    private final IEnviadorPeticiones enviadorPeticiones;
    private final IGestorRespuesta gestorRespuesta;

    public FachadaLobby() {
        this.gestorSesion = GestorSesionUsuario.getInstancia();
        this.especialistaUsuarios = new EspecialistaUsuariosImpl();
        this.enviadorPeticiones = new EnviadorPeticiones();
        this.gestorRespuesta = GestorRespuesta.getInstancia();
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
    public CompletableFuture<Boolean> cerrarSesion() {
        System.out.println("[FachadaLobby] Cerrando sesión del usuario...");

        CompletableFuture<Boolean> future = new CompletableFuture<>();

        if (!haySesionActiva()) {
            System.out.println("⚠️ [FachadaLobby]: No hay sesión activa para cerrar");
            future.complete(true);
            return future;
        }

        String usuarioId = obtenerUserIdSesion();

        // Actualizar estado local a OFFLINE antes de enviar al servidor
        try {
            UUID userId = UUID.fromString(usuarioId);
            especialistaUsuarios.actualizarEstadoUsuario(userId, "inactivo");
            System.out.println("✅ [FachadaLobby]: Estado local actualizado a OFFLINE");
        } catch (Exception e) {
            System.err.println("⚠️ [FachadaLobby]: Error al actualizar estado local: " + e.getMessage());
        }

        // Enviar petición al servidor
        JsonObject payload = new JsonObject();
        payload.addProperty("usuarioId", usuarioId);
        DTORequest request = new DTORequest("logout", payload);

        // Registrar manejador para la respuesta
        gestorRespuesta.registrarManejador("logout", (DTOResponse respuesta) -> {
            System.out.println("📥 [FachadaLobby]: Respuesta de logout recibida - Status: " + respuesta.getStatus());

            // Cerrar sesión local independientemente de la respuesta del servidor
            gestorSesion.cerrarSesion();
            System.out.println("✅ [FachadaLobby]: Sesión local cerrada");

            future.complete(respuesta.fueExitoso());
        });

        // Enviar petición
        enviadorPeticiones.enviar(request);
        System.out.println("📤 [FachadaLobby]: Petición de logout enviada al servidor");

        // Timeout de 3 segundos - si el servidor no responde, cerrar sesión local de todos modos
        CompletableFuture.delayedExecutor(3, java.util.concurrent.TimeUnit.SECONDS).execute(() -> {
            if (!future.isDone()) {
                System.out.println("⚠️ [FachadaLobby]: Timeout esperando respuesta del servidor, cerrando sesión local");
                gestorSesion.cerrarSesion();
                future.complete(true);
            }
        });

        return future;
    }
}
