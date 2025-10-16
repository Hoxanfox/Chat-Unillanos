package controlador.usuario;

import dto.vistaLobby.DTOUsuario;
import java.util.concurrent.CompletableFuture;

public interface IControladorUsuario {
    CompletableFuture<DTOUsuario> obtenerInformacionUsuario(String userId);
    CompletableFuture<Void> actualizarInformacionUsuario(DTOUsuario dtoUsuario);
    CompletableFuture<Void> guardarUsuario(DTOUsuario dtoUsuario);
    CompletableFuture<DTOUsuario> cargarInformacionUsuarioLogueado();
    boolean haySesionActiva();
    String obtenerUserIdSesion();
    void cerrarSesion();
}
