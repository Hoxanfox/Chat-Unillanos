package servicio.usuario;

import dto.vistaLobby.DTOUsuario;
import java.util.concurrent.CompletableFuture;

public interface IServicioUsuario {
    CompletableFuture<DTOUsuario> obtenerInformacionUsuario(String userId);
    CompletableFuture<Void> actualizarInformacionUsuario(DTOUsuario dtoUsuario);
    CompletableFuture<Void> guardarUsuario(DTOUsuario dtoUsuario);

    // Nuevos métodos para sesión
    CompletableFuture<DTOUsuario> cargarInformacionUsuarioLogueado();
    boolean haySesionActiva();
    String obtenerUserIdSesion();
    void cerrarSesion();
}
